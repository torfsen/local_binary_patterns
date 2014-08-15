# Texture Classification using Local Binary Patterns

The task of automatically assigning one of several categories to a texture
(i.e. an image of a pattern) is called texture classification. For example,
one might want to classify a photo of a tree bark with the corresponding
species' names. In machine learning, this problem is solved by learning the
classification from a set of samples (the training data). After learning, the
algorithm can then (try to) classify previously unseen textures.

One of the many possible techniques for doing this has been proposed by Ojala
et al. in their heavily cited paper

> *Multiresolution Gray-Scale and Rotation Invariant Texture Classification
> with Local Binary Patterns* (IEEE Transactions on Pattern Analysis and
> Machine Intelligence, Vol. 24, No. 7, July 2002).

This is a Java implementation of that approach. Note that while fully
functional, the implementation is not intended for large-scale use. Typical
input sizes are 32x32 pixels.

See also [an implementation for MATLAB][matlab] from the University of Oulu
and [one in Python][python] from Luis Pedro Coelho.

[matlab]: http://www.cse.oulu.fi/CMV/Downloads/LBPMatlab
[python]: http://luispedro.org/software/mahotas
