package com.broski.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.controllers.Controller;
import com.badlogic.gdx.controllers.Controllers;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.viewport.FitViewport;

public class ControllerLogic {


    Vector2 touchPos;

    public ControllerLogic() {
        touchPos = new Vector2();
    }

    public void input(Sprite bucketSprite, FitViewport viewport) {
        float speed = 4f;
        float delta = Gdx.graphics.getDeltaTime();

        if (Gdx.input.isKeyPressed(Input.Keys.RIGHT)) {
            bucketSprite.translateX(speed * delta);
        } else if (Gdx.input.isKeyPressed(Input.Keys.LEFT)) {
            bucketSprite.translateX(-speed * delta);
        }

        if (Gdx.input.isTouched()) {
            touchPos.set(Gdx.input.getX(), Gdx.input.getY()); // Get where the touch happened on screen
            viewport.unproject(touchPos); // Convert the units to the world units of the viewport
            bucketSprite.setCenterX(touchPos.x); // Change the horizontally centered position of the bucket
        }

        // Get the first controller
        if (Controllers.getControllers().size > 0){
            Controller controller = Controllers.getControllers().first();

            // Check if the controller is connected
            if (controller != null) {
                // Handle Xbox controller buttons and axes
                if (controller.getButton(controller.getMapping().buttonB)) {
                    bucketSprite.translateX(speed * delta);
                }
                if (controller.getButton(controller.getMapping().buttonX)) {
                    bucketSprite.translateX(-speed * delta);
                }


                float axisX = controller.getAxis(controller.getMapping().axisLeftX);

                if (axisX > 0.1f) {
                    // Move the sprite to the right
                    bucketSprite.translateX(speed * delta);
                }
                else if (axisX < -0.1f) {
                    // Move the sprite to the left
                    bucketSprite.translateX(-speed * delta );
                }


            }


        }

    }


}
