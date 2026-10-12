package com.engine.model;

import com.engine.exception.ValidationException;

/**
 * Immutable value object representing physical dimensions in centimeters.
 */
public record Dimensions(double lengthCm, double widthCm, double heightCm) {
    public Dimensions {
        if (lengthCm <= 0 || widthCm <= 0 || heightCm <= 0) {
            throw new ValidationException(String.format(
                    "Dimensions must be strictly positive: [%.2f, %.2f, %.2f]",
                    lengthCm, widthCm, heightCm));
        }
    }

    /**
     * Calculates volume in cubic centimeters.
     */
    public double volumeCc() {
        return lengthCm * widthCm * heightCm;
    }
}
