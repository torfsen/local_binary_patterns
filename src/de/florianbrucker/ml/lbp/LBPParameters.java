/*
LBPParameters - Helper class for LBPModel

Copyright (c) 2011 Florian Brucker (www.florianbrucker.de)

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
*/

package de.florianbrucker.ml.lbp;

/**
 * Encapsulates parameter settings for an {@link LBPModel}.
 * <p>
 * When using the {@link LBPModel} class for texture classification, all models
 * and samples need to be built using the same set of parameters. The purpose
 * of this class is to encapsulate these parameter settings, so that they can
 * easily be applied to several models.
 * <p>
 * See the documentation for {@link LBPModel} for tips on choosing actual
 * parameter values.
 */
public class LBPParameters {

    /** Number of neighbors */
    protected int p[];

    /** Radii */
    protected int r[];

    /** Number of bins for variance histograms */
    protected int b[];

    /**
     * Constructs a parameters object for multiple sets of parameters.
     * <p>
     * If you do not want to take variance data into account, set the number of
     * variance histogram bins to zero.
     *
     * @param neighbors Number of neighbos to take into account
     * @param radii Radius to use
     * @param bins Number of bins for variance histogram
     */
    public LBPParameters(int neighbors[], int radii[], int bins[]) {
        if (neighbors.length != radii.length ||
            neighbors.length != bins.length) {
            throw new IllegalArgumentException(
                    "Arrays must be of the same length");
        }
        p = neighbors.clone();
        r = radii.clone();
        b = bins.clone();
        sort();
    }

    /**
     * Constructs a parameter object for a single set of parameters.
     * <p>
     * If you do not want to take variance data into account, set the number of
     * variance histogram bins to zero.
     *
     * @param neighbors Number of neighbos to take into account
     * @param radius Radius to use
     * @param bins Number of bins for variance histogram
     */
    public LBPParameters(int neighbors, int radius, int bins) {
        this(new int[] { neighbors }, new int[] { radius }, new int[] { bins });
    }

    /**
     * Constructs a parameter object from a string.
     * <p>
     * This is used internally for loading parameters from a file. The string
     * format corresponds to that used by the <code>toString()</code> method.
     *
     * @param s Parameter string
     */
    protected LBPParameters(String s) {
        String fields[] = s.split(":");
        p = new int[fields.length];
        r = new int[fields.length];
        b = new int[fields.length];

        for (int i = 0; i < fields.length; i++) {
            String subs[] = fields[i].split("/");
            if (subs.length != 3) {
                throw new IllegalArgumentException("Invalid parameter string");
            }
            try {
                p[i] = Integer.parseInt(subs[0]);
                r[i] = Integer.parseInt(subs[1]);
                b[i] = Integer.parseInt(subs[2]);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid parameter string");
            }
        }
        sort();
    }

    /**
     * Sorts the parameter arrays.
     * <p>
     * This is done to make comparisons of two <code>LBPParameters</code>
     * objects easier.
     */
    protected void sort() {
        boolean sorted = false;
        while (!sorted) {
            sorted = true;
            for (int i = 0; i < p.length - 1; i++) {
                if (isLargerThan(i, i + 1)) {
                    // Swap
                    int dummy = p[i];
                    p[i] = p[i + 1];
                    p[i + 1] = dummy;
                    dummy = r[i];
                    r[i] = r[i + 1];
                    r[i + 1] = dummy;
                    dummy = b[i];
                    b[i] = b[i + 1];
                    b[i + 1] = dummy;
                    sorted = false;
                }
            }
        }
    }

    /**
     * Compares two entries in the parameter arrays.
     *
     * @param i Index of first entry
     * @param j Index of second entry
     * @return True if the first entry is larger than the second entry
     */
    protected boolean isLargerThan(int i, int j) {
        if (p[i] > p[j]) {
            return true;
        } else if (p[i] < p[j]) {
            return false;
        } else {
            if (r[i] > r[j]) {
                return true;
            } else if (r[i] < r[j]) {
                return false;
            } else {
                if (b[i] > b[j]) {
                    return true;
                } else {
                    return false;
                }
            }
        }
    }

    /**
     * Returns the number of parameter sets within this object.
     *
     * @return The number of parameter sets within this object.
     */
    public int size() {
        return p.length;
    }

    /**
     * Checks whether an object is equal to this object.
     *
     * @param o Object to check
     * @return True if both objects are LBPParameters with equal parameters
     */
    @Override
    public boolean equals(Object o) {
        LBPParameters params = null;
        try {
            params = (LBPParameters) o;
        } catch (ClassCastException e) {
            return false;
        }
        if (p.length != params.p.length) {
            return false;
        }
        for (int i = 0; i < p.length; i++) {
            if (p[i] != params.p[i] || r[i] != params.r[i] || b[i] != params.b[i]) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns a copy of the number of neighbors parameters.
     *
     * @return A copy of the number of neighbors parameters.
     */
    public int[] getNumberOfNeighbors() {
        return p.clone();
    }

    /**
     * Returns a copy of the radii parameters.
     *
     * @return A copy of the radii parameters.
     */
    public int[] getRadii() {
        return r.clone();
    }

    /**
     * Returns a copy of the number of bins in the variance histograms.
     *
     * @return A copy of the number of bins in the variance histograms.
     */
    public int[] getNumberOfVarHistBins() {
        return b.clone();
    }

    /**
     * Returns a string representation of this object.
     *
     * @return A string representation of this object.
     */
    @Override
    public String toString() {
        StringBuilder s = new StringBuilder();
        for (int i = 0; i < p.length; i++) {
            s.append(p[i]);
            s.append("/");
            s.append(r[i]);
            s.append("/");
            s.append(b[i]);
            if (i < p.length - 1) {
                s.append(":");
            }
        }
        return s.toString();
    }
}
