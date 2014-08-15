/*
LBPSubModel - Helper class for LBPModel

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

import java.awt.image.Raster;
import java.util.Arrays;
import java.util.PriorityQueue;
import java.util.Random;

/**
 * Encapsulates internal data and mechanisms for {@link LBPModel}.
 * <p>
 * Usually a single {@link LBPModel} uses several sub-models, one for each set
 * of parameters. This class encapsulates such a sub-model, and hence contains
 * most of the actual routines for building LBP models.
 * <p>
 * This class is not meant to be used on its own. Use {@link LBPModel} instead.
 */
class LBPSubModel {
    /** Radius */
    protected int r;

    /** Number of neighbors */
    protected int p;

    /** Flag showing whether this model uses local variance data */
    protected int b;

    /** Normalized pattern histogram */
    protected float patternHist[] = null;

    /** Normalized variance histogram */
    protected float varHist[] = null;

    /** Number of images incorporated into this model */
    protected int imageCount = 0;

    /**
     * Creates a sub-model.
     *
     * If you do not want to use local variance data, set the number of bins to
     * 0.
     *
     * @param p Number of neighbors
     * @param r Radius
     * @param b Number of bins for variance histogram
     */
    protected LBPSubModel(int p, int r, int b) {
        this.p = p;
        this.r = r;
        this.b = b;
    }

    /**
     * Updates the model by incorporating information from an image.
     *
     * @param raster Raster
     */
    protected void incorporate(Raster raster) {
        // Initialize variables
        int width = raster.getWidth();
        int height = raster.getHeight();
        float[] vars = null;
        if (b > 0) {
            int numPixels = (width - 2 * r) * (height - 2 * r);
            vars = new float[numPixels];
        }
        float g[] = new float[p];
        int s[] = new int[p];
        long rawPatternHist[] = new long[p + 2];
        Arrays.fill(rawPatternHist, 0);

        // Calculate information
        for (int x = r; x < width - r; x++) {
            for (int y = r; y < height - r; y++) {
                float gc = raster.getSampleFloat(x, y, 0) / 255;
                loadCircle(r, raster, x, y, g);
                rawPatternHist[lbpriu2(gc, g, s)]++;
                if (b > 0) {
                    int i = (x - r) * (height - 2 * r) + (y - r);
                    vars[i] = var(g);
                }
            }
        }

        // Update model
        updatePatternHistogram(rawPatternHist);
        if (b > 0) {
            updateVarianceHistogram(vars);
        }
        imageCount++;
    }

    /**
     * Calculates a goodness-of-fit statistic.
     * <p>
     * This method can be used to check how well a sample (represented by this
     * object) matches a model (represented by the parameter). The higher the
     * return value, the better the match. It is based on the G statistic, a
     * log-likelihood ratio test.
     * <p>
     * Note that the statistic is not symmetric, that is,
     * <br><br>
     * <code>m1.goodnessOfFit(m2) != m2.goodnessOfFit(m1)</code>
     * <br><br>
     * in the general case.
     * <p>
     * The statistic used in the paper suffers from problems if the model's
     * variance histogram contains empty cells. In that case, even a tiny
     * amount of data in the corresponding sample cell results in a statistic
     * of -infinity. Therefore, this implementation only takes histogram cells
     * into account which are non-empty for both sample and model.
     *
     * @param m The model
     * @return Goodness-of-fit statistic
     */
    protected float goodnessOfFit(LBPSubModel m) {
        if (m.p != p || m.r != r || m.b != b) {
            throw new IllegalArgumentException(
                    "Model and sample parameters differ");
        }
        if (imageCount == 0) {
            // Empty sample
            throw new IllegalStateException("Sample contains no data");
        }
        float gof = 0;
        if (b == 0) {
            for (int i = 0; i < p + 2; i++) {
                if (m.patternHist[i] > 0) {
                    gof += patternHist[i] * Math.log(m.patternHist[i]);
                }
            }
        } else {
            for (int i = 0; i < p + 2; i++) {
                for (int j = 0; j < b; j++) {
                    float p = m.patternHist[i] * m.varHist[j];
                    if (p > 0) {
                        gof += patternHist[i] * varHist[j] * Math.log(p);
                    }
                }
            }
        }
        return gof;
    }

    /**
     * Updates the internal pattern histogram.
     *
     * @param rawHist Raw pattern data
     */
    protected void updatePatternHistogram(long rawHist[]) {
        long sum = 0;
        for (int i = 0; i < p + 2; i++) {
            sum += rawHist[i];
        }
        if (imageCount == 0) {
            // This is the first time we add histogram data
            patternHist = new float[p + 2];
            for (int i = 0; i < p + 2; i++) {
                patternHist[i] = (float) (rawHist[i] / ((double) sum));
            }
        } else {
            for (int i = 0; i < p + 2; i++) {
                float x = (float) (rawHist[i] / ((double) sum));
                patternHist[i] = (imageCount * patternHist[i] + x) /
                        (imageCount + 1);
            }
        }
    }

    /**
     * Creates a variance histogram from scratch.
     * <p>
     * This method creates a histogram for the variance values passed via parameter.
     * <p>
     * We use a logarithmically spaced histogram for the variance that has
     * fixed bin edges (in contrast to the paper, where variable bin edges
     * are used). Fixed bin edges have the advantage that comparing and
     * updating models is much simpler. The logarithmical spacing is due
     * to experiments with the variable bin size algorithm from the paper,
     * which produces approximately logarithmically spaced bins.
     * 
     * @param vars Variance values
     * @return Variance histogram
     */
    protected float[] createVarianceHistogram(float vars[]) {
        float h[] = new float[b];
        for (int i = 0; i < vars.length; i++) {
            float f = (float) (Math.max(Math.log10(vars[i]), -6) + 6) / 6;
            int bin = (int) (b * f - 0.000001);
            h[bin]++;
        }
        for (int i = 0; i < b; i++) {
            h[i] /= vars.length;
        }
        return h;
    }

    /**
     * Updates the internal variance histogram.
     * 
     * @param vars Variance data
     */
    protected void updateVarianceHistogram(float[] vars) {
        if (imageCount == 0) {
            // This is the first time we add histogram data
            varHist = createVarianceHistogram(vars);
        } else {
            float[] newHist = createVarianceHistogram(vars);
            for (int i = 0; i < b; i++) {
                varHist[i] = (imageCount * varHist[i] + newHist[i]) /
                        (imageCount + 1);
            }
        }
    }

    /**
     * Bilinear interpolation for Raster data.
     * <p>
     * Note that no out-of-bounds checking is performed.
     *
     * @param raster Raster
     * @param x x-coordinate
     * @param y y-coordinate
     * @param b Band number
     * @return pixel value of the given band at the given location
     */
    protected static float interpolate(Raster raster, float x, float y, int b) {
        int x1, x2, y1, y2;

        /*
         * We need to check whether the coordinates lie directly on a pixel
         * center. Otherwise we might run into out-of-bounds errors at the
         * edges of the raster.
         */
        int xr = Math.round(x);
        if (Math.abs(x - xr) < 1e-4) {
            x1 = xr;
            x2 = xr;
            x = 0;
        } else {
            x1 = (int) Math.floor(x);
            x2 = x1 + 1;
            x = x - x1;
        }
        int yr = Math.round(y);
        if (Math.abs(y - yr) < 1e-4) {
            y1 = yr;
            y2 = yr;
            y = 0;
        } else {
            y1 = (int) Math.floor(y);
            y2 = y1 + 1;
            y = y - y1;
        }

        float ll = raster.getSampleFloat(x1, y1, b) / 255;
        float ul = raster.getSampleFloat(x1, y2, b) / 255;
        float lr = raster.getSampleFloat(x2, y1, b) / 255;
        float ur = raster.getSampleFloat(x2, y2, b) / 255;
        return (
            ll * (1 - x) * (1 - y) 
            + lr * x * (1 - y) 
            + ul * (1 - x) * y 
            + ur * x * y
        );
    }

    /**
     * Loads a circle of pixels from an image.
     * <p>
     * Note that no out-of-bounds checking is performed.
     *
     * @param r Radius
     * @param raster Raster
     * @param x x-coordinate of the center
     * @param y y-coordinate of the center
     * @param g Pre-allocated array of neighbors
     */
    protected static void loadCircle(int r, Raster raster, int x, int y,
                                     float g[]) {
        int p = g.length;
        float f = 2 * (float) Math.PI / p;
        for (int i = 0; i < p; i++) {
            float gx =  x - r * (float) Math.sin(i * f);
            float gy = y + r * (float) Math.cos(i * f);
            g[i] = interpolate(raster, gx, gy, 0);
        }
    }

    /**
     * Calculates the local, rotation invariant, and uniform binary pattern for
     * a single pixel.
     *
     * @param gc Center pixel value
     * @param g Circle pixels as returned from loadCircle
     * @param s Pre-allocated integer array of length p
     * @return The local binary pattern
     */
    protected static int lbpriu2(float gc, float g[], int s[]) {
        int p = g.length;
        for (int i = 0; i < p; i++) {
            s[i] = g[i] - gc >= 0 ? 1 : 0;
        }
        // Equation (10) in the paper
        int u = 0;
        for (int i = 0; i < p; i++) {
            u += s[i] == s[(i + 1) % p] ? 0 : 1;
        }
        // Equation (9) in the paper
        if (u <= 2) {
            int sum = 0;
            for (int i = 0; i < p; i++) {
                sum += s[i];
            }
            return sum;
        } else {
            return p + 1;
        }
    }

    /**
     * Calculates the rotation invariant measure of local variance.
     *
     * @param g Circle pixels as returned from loadCircle
     * @return The local variance
     */
    protected static float var(float g[]) {
        float mu = 0;
        int p = g.length;
        for (int i = 0; i < p; i++) {
            mu += g[i];
        }
        mu /= p;
        float v = 0;
        for (int i = 0; i < p; i++) {
            v += (g[i] - mu) * (g[i] - mu);
        }
        return v / p;
    }

    /**
     * Returns a string representation of this sub-model.
     *
     * @return A string representation of this sub-model.
     */
    @Override
    public String toString() {
        StringBuilder s = new StringBuilder("");
        for (int i = 0; i < p + 2; i++) {
            s.append(patternHist[i]);
            if (i < p + 1) {
                s.append("/");
            }
        }
        s.append(":");
        for (int i = 0; i < b; i++) {
            s.append(varHist[i]);
            if (i < b - 1) {
                s.append("/");
            }
        }
        return s.toString();
    }

    /**
     * Loads histogram data from a string.
     *
     * This method is used internally to load a sub-model from a file. The
     * string format corresponds to that returned by the toString() method.
     *
     * @param s String with histogram data
     */
    protected void loadFromString(String s) {
        String fields[] = s.split(":");
        if (fields.length != 2) {
            throw new IllegalArgumentException("Invalid base model string");
        }
        String subs[] = fields[0].split("/");
        if (subs.length != p + 2) {
            throw new IllegalArgumentException("Invalid base model string");
        }
        patternHist = new float[p + 2];
        for (int i = 0; i < p + 2; i++) {
            try {
                patternHist[i] = Float.parseFloat(subs[i]);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid base model string");
            }
        }
        if (b > 0) {
            subs = fields[1].split("/");
            if (subs.length != b) {
                throw new IllegalArgumentException("Invalid base model string");
            }
            varHist = new float[b];
            for (int i = 0; i < b; i++) {
                try {
                    varHist[i] = Float.parseFloat(subs[i]);
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException(
                            "Invalid base model string");
                }
            }
        }
    }
}
