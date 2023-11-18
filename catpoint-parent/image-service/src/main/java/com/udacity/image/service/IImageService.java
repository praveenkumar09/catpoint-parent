package com.udacity.image.service;

import java.awt.image.BufferedImage;

public interface IImageService {
    boolean imageContainsCat(BufferedImage image, float confidenceThreshold);
}
