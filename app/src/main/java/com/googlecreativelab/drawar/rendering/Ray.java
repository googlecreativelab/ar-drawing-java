package com.googlecreativelab.drawar.rendering;

import javax.vecmath.Vector3f;
/**
 * Created by mooredan on 3/2/18.
 */


public class Ray {
    public final Vector3f origin;
    public final Vector3f direction;
    public Ray(Vector3f origin, Vector3f direction) {
        this.origin = origin;
        this.direction = direction;
    }
}