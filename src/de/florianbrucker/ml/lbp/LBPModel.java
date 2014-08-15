/*
LBPModel - A Java Texture Classification Class

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

import java.awt.image.*;
import java.io.*;

import javax.imageio.ImageIO;

/**
 * Represents a model for texture classification by local binary patterns.
 * <p>
 * This class is an implementation of the algorithms in the article
 * <br><br>
 * <i>Ojala, Pietikäinen, Mäenpää: "Multiresolution Gray-Scale and Rotation
 * Invariant Texture Classification with Local Binary Patterns", IEEE
 * Transactions on Pattern Analysis and Machine Intelligence, Vol. 24, No. 7,
 * July 2002</i>
 * <p>
 * This class can be used to construct both models for texture classes and
 * samples that are to be classified. Use the <code>classify()</code> method on
 * an <code>LBPModel</code> representing a sample to automatically classify it
 * against a set of models.
 * <p>
 * Ojala et al. suggest using the following combinations of number of neighbors
 * <code>P</code> and radius <code>R</code>: <code>(N = 8, R = 1),
 * (N = 16, R = 2), (N = 24, R = 3)</code>. See the paper for a detailed
 * explanation of the algorithms and parameters.
 * <p>
 * See {@link LBPDemo} for a simple demo application using this class.
 * <p>
 * This implementation uses a logarithmically spaced histogram for the variance
 * data that has fixed bin edges (in contrast to the paper, where variable bin
 * edges are used). Fixed bin edges have the advantage that comparing and
 * updating models is much simpler. The logarithmical spacing is due to
 * experiments with the variable bin size algorithm from the paper, which
 * produces approximately logarithmically spaced bins.
 * <p>
 * A second difference between the original paper and this implementation is
 * the goodness-of-fit statistic, see the documentation of the
 * <code>goodnessOfFit()</code> method.
 * <p>
 * Note that this whole class takes only the first band of any image into
 * account. You may use the <code>incorporate()</code> method to pass
 * additional bands manually.
 */
public class LBPModel {

    /** Parameters */
    protected LBPParameters params;

    /** Sub-models */
    protected LBPSubModel subModels[];

    /**
     * Initializes all sub-modules according to the parameters.
     */
    protected void initSubModels() {
        subModels = new LBPSubModel[params.size()];
        for (int i = 0; i < subModels.length; i++) {
            subModels[i] = new LBPSubModel(params.p[i], params.r[i],
                                           params.b[i]);
        }
    }

    /**
     * Creates a LBP model without any data.
     *
     * @param p Parameters
     */
    public LBPModel(LBPParameters p) {
        this(p, new BufferedImage[0]);
    }

    /**
     * Creates a LBP model from a set of images.
     *
     * @param p Parameters
     * @param images Images
     */
    public LBPModel(LBPParameters p, BufferedImage images[]) {
        params = p;
        initSubModels();
        for (int i = 0; i < images.length; i++) {
            incorporate(images[i]);
        }
    }

    /**
     * Creates a LBP model based upon a single image.
     * 
     * @param p Parameters
     * @param img Image
     */
    public LBPModel(LBPParameters p, BufferedImage img) {
        this(p, new BufferedImage[] { img });
    }

    /**
     * Creates a LBP model based upon a single image that is loaded from a file.
     * 
     * @param p Parameters
     * @param file Image file
     * @throws IOException If image cannot be read
     */
    public LBPModel(LBPParameters p, File file) throws IOException {
        this(p, new File[] { file });
    }

    /**
     * Creates a LBP model based upon images that are loaded from files.
     * <p>
     * Use this constructor if your texture data is stored on disk. The images
     * are loaded sequentially, so memory usage is kept low.
     *
     * @param p Parameters
     * @param files Image files
     * @throws IOException If one of the images cannot be read
     */
    public LBPModel(LBPParameters p, File files[]) throws IOException {
        params = p;
        initSubModels();
        for (int i = 0; i < files.length; i++) {
            incorporate(files[i]);
        }
    }

    /**
     * Incorporates data from an image into the model.
     *
     * @param raster Image data
     */
    public void incorporate(Raster raster) {
        for (int j = 0; j < subModels.length; j++) {
            subModels[j].incorporate(raster);
        }
    }

    /**
     * Incorporates data from an image into the model.
     *
     * @param img Image data
     */
    public void incorporate(BufferedImage img) {
        incorporate(img.getData());
    }

    /**
     * Incorporates data from an image into the model.
     * 
     * @param file Image file
     */
    public void incorporate(File file) throws IOException {
        incorporate(ImageIO.read(file).getData());
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
     * @param model The model
     * @return Goodness-of-fit statistic
     */
    public float goodnessOfFit(LBPModel model) {
        float gof = 0;
        for (int i = 0; i < subModels.length; i++) {
            gof += subModels[i].goodnessOfFit(model.subModels[i]);
        }
        return gof;
    }

    /**
     * Classifies this sample.
     * <p>
     * This method calculates the goodness-of-fit statistic for each of the
     * given models and returns the index of the model for which the statistic
     * is maximal.
     *
     * @param models Models
     * @return Index of the model with the largest statistic
     */
    public int classify(LBPModel models[]) {
        int best = 0;
        float maxgof = Float.NEGATIVE_INFINITY;
        for (int i = 0; i < models.length; i++) {
            float gof = goodnessOfFit(models[i]);
            if (gof > maxgof) {
                maxgof = gof;
                best = i;
            }
        }
        return best;
    }

    /**
     * Stores this model in a file.
     *
     * @param f Target file
     * @throws IOException If writing to the file failed
     */
    public void save(File f) throws IOException {
        BufferedWriter out = new BufferedWriter(new FileWriter(f));
        out.write(params.toString() + "\n");
        for (int i = 0; i < subModels.length; i++) {
            out.write(subModels[i].toString() + "\n");
        }
        out.close();
    }

    /**
     * Load a LBP model from a file.
     *
     * @param f File
     * @throws IOException If the model data cannot be read
     */
    public LBPModel(File f) throws IOException {
        BufferedReader in = new BufferedReader((new FileReader(f)));
        String s = in.readLine();
        if (s == null) {
            throw new IllegalArgumentException("Invalid model file");
        }
        params = new LBPParameters(s);
        subModels = new LBPSubModel[params.size()];
        for (int i = 0; i < params.size(); i++) {
            s = in.readLine();
            if (s == null) {
                throw new IllegalArgumentException("Invalid model file");
            }
            subModels[i] = new LBPSubModel(params.p[i], params.r[i],
                                           params.b[i]);
            subModels[i].loadFromString(s);
        }
        in.close();
    }

    /**
     * Returns the model parameters.
     * @return The model parameters.
     */
    public LBPParameters getParameters() {
        return params;
    }
}
