package com.broski.game;

import com.badlogic.gdx.*;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.controllers.ControllerAdapter;
import com.badlogic.gdx.controllers.ControllerListener;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.controllers.Controller;
import com.badlogic.gdx.controllers.Controllers;
import com.badlogic.gdx.controllers.ControllerMapping.*;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import static com.badlogic.gdx.scenes.scene2d.InputEvent.Type.exit;
import static java.lang.System.exit;

public class Broski extends ApplicationAdapter {


    private BitmapFont font;
    private float survivalTime = 0f;
    private float highScore = 0f;
    private final String highScoreFile = "highscore.txt";  // Name of the file to store high score


    private ShapeRenderer shapeRenderer;
    private SpriteBatch batch;

    private Vector2 ball1Pos;
    private Vector2 ball2Pos;
    private float ballRadius = 20f;


    private float enemySpawnTimer = 5f;  // Initial spawn interval in seconds
    private float timeSinceLastSpawn = 0f;
    private int enemiesToSpawn = 1;  // Start by spawning one enemy at a time
    private float spawnIntervalReduction = 0.1f;  // Reduce the interval slightly with each spawn

    private int killsPlayer1 = 0;
    private int killsPlayer2 = 0;
    private int rocketsPlayer1 = 0;
    private int rocketsPlayer2 = 0;

    private class Rocket {
        Vector2 position;
        Vector2 velocity;
        boolean isActive;

        public Rocket(Vector2 position, Vector2 direction) {
            this.position = new Vector2(position);
            this.velocity = new Vector2(direction).nor().scl(10f);  // Set rocket speed
            this.isActive = true;
        }

        public void update() {
            if (isActive) {
                position.add(velocity);  // Move the rocket
            }
        }

        public boolean isOffScreen() {
            return position.x < 0 || position.x > Gdx.graphics.getWidth() || position.y < 0 || position.y > Gdx.graphics.getHeight();
        }
    }

    private Rocket rocketPlayer1 = null;
    private Rocket rocketPlayer2 = null;

    private void launchRocket(Controller controller, Vector2 playerPos, int playerNumber) {
        // Check if the left trigger value is >= 0.5 to trigger rocket launch
        if (controller.getAxis(4) >= 0.5f) {
            if (playerNumber == 1 && rocketsPlayer1 > 0 && rocketPlayer1 == null) {
                // Launch Player 1's rocket
                Vector2 direction = new Vector2(controller.getAxis(0), -controller.getAxis(1));
                if (direction.len() == 0) {
                    direction.set(1, 0);  // Default to shooting right if no movement input
                }
                rocketPlayer1 = new Rocket(playerPos, direction);
                rocketsPlayer1--;  // Use up one rocket
            } else if (playerNumber == 2 && rocketsPlayer2 > 0 && rocketPlayer2 == null) {
                // Launch Player 2's rocket
                Vector2 direction = new Vector2(controller.getAxis(0), -controller.getAxis(1));
                if (direction.len() == 0) {
                    direction.set(1, 0);  // Default to shooting right if no movement input
                }
                rocketPlayer2 = new Rocket(playerPos, direction);
                rocketsPlayer2--;  // Use up one rocket
            }
        }
    }



    private void checkRocketEnemyCollision(Rocket rocket, int playerNumber) {
        if (rocket == null || !rocket.isActive) return;

        Iterator<EnemyBall> enemyIterator = enemyBalls.iterator();
        while (enemyIterator.hasNext()) {
            EnemyBall enemy = enemyIterator.next();
            if (rocket.position.dst(enemy.position) < ballRadius * 2) {  // Rocket hits enemy
                // Trigger explosion at the rocket's position
                activeExplosions.add(new Explosion(rocket.position, 150, 3f));  // Adjust explosion radius as needed
                rocket.isActive = false;  // Rocket is no longer active
                break;
            }
        }

        // Remove the rocket if it goes off the screen and trigger explosion
        if (rocket.isOffScreen()) {
            activeExplosions.add(new Explosion(rocket.position, 150, 3f));  // Explosion at the edge of the screen
            rocket.isActive = false;
        }

        // Reset the rocket to null if it becomes inactive
        if (!rocket.isActive) {
            if (playerNumber == 1) {
                rocketPlayer1 = null;  // Reset player 1's rocket
            } else if (playerNumber == 2) {
                rocketPlayer2 = null;  // Reset player 2's rocket
            }
        }
    }




    private class Explosion {
        Vector2 position;
        float radius;
        float timeRemaining;

        public Explosion(Vector2 position, float radius, float duration) {
            this.position = position;
            this.radius = radius;
            this.timeRemaining = duration;
        }

        public void update(float deltaTime) {
            timeRemaining -= deltaTime;  // Decrease time remaining
        }

        public boolean isFinished() {
            return timeRemaining <= 0;
        }

        public boolean isInRange(Vector2 enemyPos) {
            return position.dst(enemyPos) < radius;  // Check if enemy is within the explosion radius
        }
    }

    private List<Explosion> activeExplosions = new ArrayList<>();

    private void checkRocketEarned() {
        // Check player 1
        if (killsPlayer1 >= 5) {
            rocketsPlayer1++;
            killsPlayer1 -= 5;  // Reset the counter to track the next 5 kills
        }

        // Check player 2
        if (killsPlayer2 >= 5) {
            rocketsPlayer2++;
            killsPlayer2 -= 5;  // Reset the counter to track the next 5 kills
        }
    }



    private void useRocket(Vector2 playerPos, int playerRockets) {
        if (playerRockets > 0) {
            // Create a new explosion at the player's position
            activeExplosions.add(new Explosion(playerPos, 150, 3f));  // Adjust the radius and duration as needed
            playerRockets--;  // Decrease the number of rockets
        }
    }

    private void updateExplosions(float deltaTime) {
        Iterator<Explosion> explosionIterator = activeExplosions.iterator();
        while (explosionIterator.hasNext()) {
            Explosion explosion = explosionIterator.next();
            explosion.update(deltaTime);

            // Check if the explosion has expired
            if (explosion.isFinished()) {
                explosionIterator.remove();
                continue;
            }

            // Check if any enemies are within the explosion radius
            Iterator<EnemyBall> enemyIterator = enemyBalls.iterator();
            while (enemyIterator.hasNext()) {
                EnemyBall enemy = enemyIterator.next();
                if (explosion.isInRange(enemy.position)) {
                    enemyIterator.remove();  // Kill the enemy
                }
            }
        }
    }


    private void renderExplosions() {
        shapeRenderer.setColor(Color.ORANGE);  // Explosion color
        for (Explosion explosion : activeExplosions) {
            shapeRenderer.circle(explosion.position.x, explosion.position.y, explosion.radius);  // Render explosion circle
        }
    }



    private void spawnEnemies(int numberOfEnemies) {
        for (int i = 0; i < numberOfEnemies; i++) {
            Vector2 spawnPosition;
            boolean isTooClose;

            // Keep generating a position until it's far enough from both players
            do {
                float randomX = random.nextInt(Gdx.graphics.getWidth());
                float randomY = random.nextInt(Gdx.graphics.getHeight());
                spawnPosition = new Vector2(randomX, randomY);

                // Check distance from both players
                isTooClose = (isBall1Visible && spawnPosition.dst(ball1Pos) < ballRadius * 8) ||  // Adjust distance as needed
                    (isBall2Visible && spawnPosition.dst(ball2Pos) < ballRadius * 8);    // Adjust distance as needed

            } while (isTooClose);

            // Random initial velocity
            float randomVX = (random.nextFloat() - 0.5f) * 2f;
            float randomVY = (random.nextFloat() - 0.5f) * 2f;

            // Add the new enemy to the list
            enemyBalls.add(new EnemyBall(spawnPosition.x, spawnPosition.y, randomVX, randomVY));
        }
    }


    private void updateEnemySpawning(float deltaTime) {
        timeSinceLastSpawn += deltaTime;

        // Check if it's time to spawn more enemies
        if (timeSinceLastSpawn >= enemySpawnTimer) {
            spawnEnemies(enemiesToSpawn);  // Spawn the current number of enemies
            timeSinceLastSpawn = 0f;  // Reset the spawn timer

            // Make the game harder by increasing enemies to spawn and reducing the spawn interval
            enemiesToSpawn++;
            enemySpawnTimer = Math.max(1f, enemySpawnTimer - spawnIntervalReduction);  // Ensure the spawn interval doesn't go below 1 second
        }
    }


    private class EnemyBall {
        Vector2 position;
        Vector2 velocity;
        int health = 3;  // Enemy can take 3 hits before dying

        public EnemyBall(float x, float y, float vx, float vy) {
            this.position = new Vector2(x, y);
            this.velocity = new Vector2(vx, vy);
        }

        public void update(Vector2 target) {
            // Move towards the target (similar to before)
            if (target != null) {
                Vector2 direction = new Vector2(target).sub(position).nor();
                velocity.set(direction.scl(2f));
                position.add(velocity);
            }

            // Bounce back if hitting screen boundaries
            if (position.x < ballRadius || position.x > Gdx.graphics.getWidth() - ballRadius) {
                velocity.x = -velocity.x;
            }
            if (position.y < ballRadius || position.y > Gdx.graphics.getHeight() - ballRadius) {
                velocity.y = -velocity.y;
            }
        }

        public boolean isDead() {
            return health <= 0;
        }

        public void takeDamage() {
            health--;  // Decrease health when hit by a bullet
        }
    }

    private List<EnemyBall> enemyBalls;
    private int playerScore = 0;

    private Controller ball1Controller;
    private Controller ball2Controller;

    private boolean isBall1Visible = true;
    private boolean isBall2Visible = true;

    private boolean isWeapon1Visible = true;
    private boolean isWeapon2Visible = true;
    private Random random;


    private Vector2 weapon1Pos;
    private Vector2 weapon2Pos;
    private boolean isWeapon1PickedUp = false;
    private boolean isWeapon2PickedUp = false;

    private void generateWeaponPositions() {
        weapon1Pos = new Vector2(random.nextInt(Gdx.graphics.getWidth()), random.nextInt(Gdx.graphics.getHeight()));
        weapon2Pos = new Vector2(random.nextInt(Gdx.graphics.getWidth()), random.nextInt(Gdx.graphics.getHeight()));
    }

    private void renderWeapons() {
        shapeRenderer.setColor(Color.ORANGE);  // Choose a color for the weapon

        if (isWeapon1Visible) {
            shapeRenderer.circle(weapon1Pos.x, weapon1Pos.y, ballRadius);  // Render weapon 1
        }

        if (isWeapon2Visible) {
            shapeRenderer.circle(weapon2Pos.x, weapon2Pos.y, ballRadius);  // Render weapon 2
        }
    }

    private void checkWeaponPickUp() {
        // Player 1 picks up weapon
        if (isBall1Visible && isWeapon1Visible && ball1Pos.dst(weapon1Pos) < ballRadius * 2) {
            isWeapon1PickedUp = true;
            isWeapon1Visible = false;  // Weapon disappears when picked up
        }

        // Player 2 picks up weapon
        if (isBall2Visible && isWeapon2Visible && ball2Pos.dst(weapon2Pos) < ballRadius * 2) {
            isWeapon2PickedUp = true;
            isWeapon2Visible = false;  // Weapon disappears when picked up
        }
    }


    ///Bullet class and methods
    /// --Re-structure later --
    private class Bullet {
        Vector2 position;
        Vector2 velocity;

        public Bullet(Vector2 position, Vector2 direction) {
            this.position = new Vector2(position);
            this.velocity = new Vector2(direction).nor().scl(8f);  // Set bullet speed
        }

        public void update() {
            position.add(velocity);
        }

        public boolean isOffScreen() {
            return position.x < 0 || position.x > Gdx.graphics.getWidth() || position.y < 0 || position.y > Gdx.graphics.getHeight();
        }
    }


    // Store bullets for both players
    private List<Bullet> bulletsPlayer1 = new ArrayList<>();
    private List<Bullet> bulletsPlayer2 = new ArrayList<>();

    private void shootBullet(Controller controller, Vector2 playerPos, List<Bullet> bulletList, boolean isWeaponPickedUp) {
        if (isWeaponPickedUp && controller.getAxis(5) > 0.5f) {  // Right trigger (RT) threshold
            // Get the direction the player is moving in
            Vector2 direction = new Vector2(controller.getAxis(0), -controller.getAxis(1));

            // Default to shooting forward if no input is provided
            if (direction.len() == 0) {
                direction.set(1, 0);  // Default to shooting right
            }

            // Create and shoot the bullet
            bulletList.add(new Bullet(playerPos, direction));
        }
    }


    private void updateBullets(List<Bullet> bulletList) {
        for (Bullet bullet : bulletList) {
            bullet.update();
        }
    }

    private void renderBullets(List<Bullet> bulletList) {
        shapeRenderer.setColor(Color.YELLOW);  // Color for bullets
        for (Bullet bullet : bulletList) {
            shapeRenderer.circle(bullet.position.x, bullet.position.y, 5);  // Render as small circles
        }
    }

    private void checkBulletEnemyCollision(List<Bullet> bullets, int playerNumber) {
        Iterator<Bullet> bulletIterator = bullets.iterator();
        while (bulletIterator.hasNext()) {
            Bullet bullet = bulletIterator.next();

            // Check if bullet is off the screen
            if (bullet.isOffScreen()) {
                bulletIterator.remove();  // Remove the bullet if it hits the edge of the screen
                continue;
            }

            // Check for collision with enemies
            Iterator<EnemyBall> enemyIterator = enemyBalls.iterator();
            while (enemyIterator.hasNext()) {
                EnemyBall enemy = enemyIterator.next();
                if (bullet.position.dst(enemy.position) < ballRadius) {  // Bullet hits enemy
                    enemy.takeDamage();  // Enemy takes damage
                    bulletIterator.remove();  // Bullet disappears

                    // Check if the enemy is dead
                    if (enemy.isDead()) {
                        enemyIterator.remove();  // Remove enemy if dead

                        // Increment kills for the appropriate player
                        if (playerNumber == 1) {
                            killsPlayer1++;
                        } else if (playerNumber == 2) {
                            killsPlayer2++;
                        }
                    }
                    break;  // Exit loop after processing the collision
                }
            }
        }
    }









    @Override
        public void create() {
            shapeRenderer = new ShapeRenderer();
            batch = new SpriteBatch();
            random = new Random();


        loadHighScore();

        // Initialize font for UI
        font = new BitmapFont();  // Use default font
        font.setColor(Color.WHITE);  // Set the font color to white

            ball1Pos = new Vector2(Gdx.graphics.getWidth() / 4f, Gdx.graphics.getHeight() / 2f);
            ball2Pos = new Vector2(3 * Gdx.graphics.getWidth() / 4f, Gdx.graphics.getHeight() / 2f);

            // Create some initial enemy balls with random positions and velocities
            enemyBalls = new ArrayList<>();
            for (int i = 0; i < 5; i++) {
                float randomX = random.nextInt(Gdx.graphics.getWidth());
                float randomY = random.nextInt(Gdx.graphics.getHeight());
                float randomVX = (random.nextFloat() - 0.5f) * 2f;  // Initial random velocity X
                float randomVY = (random.nextFloat() - 0.5f) * 2f;  // Initial random velocity Y
                enemyBalls.add(new EnemyBall(randomX, randomY, randomVX, randomVY));
            }

            generateWeaponPositions();


        // Assign any already connected controllers dynamically
            for (Controller controller : Controllers.getControllers()) {
                assignController(controller);
            }

            // Listen for controller connections and disconnections
            Controllers.addListener(new ControllerAdapter() {
                @Override
                public void connected(Controller controller) {
                    assignController(controller);
                }

                @Override
                public void disconnected(Controller controller) {
                    if (controller == ball1Controller) {
                        ball1Controller = null;
                        isBall1Visible = false;
                    } else if (controller == ball2Controller) {
                        ball2Controller = null;
                        isBall2Visible = false;
                    }
                }
            });
        }

        private void assignController(Controller controller) {
            // First, check if this controller was already assigned to a ball and reassign it to the same ball
            if (controller == ball1Controller) {
                isBall1Visible = true;
                return;
            } else if (controller == ball2Controller) {
                isBall2Visible = true;
                return;
            }

            // Now assign the controller to a free ball
            if (ball1Controller == null) {
                ball1Controller = controller;
                isBall1Visible = true;
            } else if (ball2Controller == null) {
                ball2Controller = controller;
                isBall2Visible = true;
            }
            // If both balls are assigned, do nothing
        }

    private void renderRocket(Rocket rocket) {
        if (rocket != null && rocket.isActive) {
            shapeRenderer.setColor(Color.RED);  // Rocket color
            shapeRenderer.rect(rocket.position.x - 10, rocket.position.y - 5, 20, 10);  // Render as a rectangle (width: 20, height: 10)
        }
    }


    private void updateRocket(Rocket rocket, int playerNumber) {
        if (rocket != null && rocket.isActive) {
            rocket.update();
        } else if (rocket != null && !rocket.isActive) {
            // Reset the rocket if it's inactive
            if (playerNumber == 1) {
                rocketPlayer1 = null;
            } else if (playerNumber == 2) {
                rocketPlayer2 = null;
            }
        }
    }



    ///Game over and high score methods
    private void checkGameOver() {
        if (!isBall1Visible && !isBall2Visible) {
            if (survivalTime > highScore) {
                highScore = survivalTime;  // Update high score
                saveHighScore();  // Save to file
                Gdx.app.exit();
            }
        }
    }
    private void loadHighScore() {
        try {
            FileHandle file = Gdx.files.local(highScoreFile);
            if (file.exists()) {
                String scoreStr = file.readString();
                highScore = Float.parseFloat(scoreStr);  // Load and parse the high score
            }
        } catch (Exception e) {
            System.out.println("Error loading high score: " + e.getMessage());
            highScore = 0f;  // Default to 0 if there's an error
        }
    }

    private void saveHighScore() {
        try {
            FileHandle file = Gdx.files.local(highScoreFile);
            file.writeString(Float.toString(highScore), false);  // Save the high score
        } catch (Exception e) {
            System.out.println("Error saving high score: " + e.getMessage());
        }
    }



    @Override
    public void render() {
        float deltaTime = Gdx.graphics.getDeltaTime();  // Time passed since the last frame

        if (isBall1Visible || isBall2Visible) {
            survivalTime += Gdx.graphics.getDeltaTime();  // Increment survival time while at least one ball is alive
        }
        checkGameOver();


        float leftTriggerValuePlayer1 = (ball1Controller != null) ? ball1Controller.getAxis(4) : 0f;
        float leftTriggerValuePlayer2 = (ball2Controller != null) ? ball2Controller.getAxis(4) : 0f;

        // Clear the screen
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // Update and spawn enemies over time
        updateEnemySpawning(deltaTime);

        // Update player balls and enemies
        updateBalls();
        updateEnemyBalls();
        checkCollision();
        checkWeaponPickUp();

        // Update bullets
        updateBullets(bulletsPlayer1);
        updateBullets(bulletsPlayer2);

        // Begin shape rendering for bullets, players, enemies, rockets, and explosions
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        // Render explosions
        renderExplosions();

        // Render bullets for both players
        renderBullets(bulletsPlayer1);
        renderBullets(bulletsPlayer2);

        // Render rockets for both players
        renderRocket(rocketPlayer1);
        renderRocket(rocketPlayer2);

        // Render weapons and player balls
        renderWeapons();
        if (isBall1Visible) {
            shapeRenderer.setColor(Color.RED);
            shapeRenderer.circle(ball1Pos.x, ball1Pos.y, ballRadius);  // Player 1
        }
        if (isBall2Visible) {
            shapeRenderer.setColor(Color.BLUE);
            shapeRenderer.circle(ball2Pos.x, ball2Pos.y, ballRadius);  // Player 2
        }

        // Render enemy balls
        shapeRenderer.setColor(Color.GREEN);
        for (EnemyBall enemy : enemyBalls) {
            shapeRenderer.circle(enemy.position.x, enemy.position.y, ballRadius);
        }

        // End shape rendering
        shapeRenderer.end();

        // Check bullet collisions with enemies and remove them if necessary
        checkBulletEnemyCollision(bulletsPlayer1, 1);  // Check collisions for Player 1 bullets
        checkBulletEnemyCollision(bulletsPlayer2, 2);  // Check collisions for Player 2 bullets

        // Update rockets and check for collisions
        updateRocket(rocketPlayer1, 1);  // Update Player 1's rocket
        updateRocket(rocketPlayer2, 2);  // Update Player 2's rocket
        checkRocketEnemyCollision(rocketPlayer1, 1);  // Check collision for Player 1's rocket
        checkRocketEnemyCollision(rocketPlayer2, 2);  // Check collision for Player 2's rocket

        // Shooting logic for both players
        if (ball1Controller != null) {
            shootBullet(ball1Controller, ball1Pos, bulletsPlayer1, isWeapon1PickedUp);
            launchRocket(ball1Controller, ball1Pos, 1);  // Launch rocket for player 1
        }
        if (ball2Controller != null) {
            shootBullet(ball2Controller, ball2Pos, bulletsPlayer2, isWeapon2PickedUp);
            launchRocket(ball2Controller, ball2Pos, 2);  // Launch rocket for player 2
        }

        // Update explosions and check for enemy deaths
        updateExplosions(deltaTime);
        checkEnemyCollision();
        // Check if players earn rockets
        checkRocketEarned();

        // UI Rendering (display rockets and kills)
        batch.begin();
        font.draw(batch, "Player 1 Rockets: " + rocketsPlayer1 + " | Kills: " + killsPlayer1 + " | LT: " + leftTriggerValuePlayer1, 10, Gdx.graphics.getHeight() - 10);
        font.draw(batch, "Player 2 Rockets: " + rocketsPlayer2 + " | Kills: " + killsPlayer2 + " | LT: " + leftTriggerValuePlayer2, Gdx.graphics.getWidth() - 250, Gdx.graphics.getHeight() - 10);
        font.draw(batch, "Survival Time: " + String.format("%.2f", survivalTime) + " seconds", 10, Gdx.graphics.getHeight() - 30);  // Display survival time
        font.draw(batch, "High Score: " + String.format("%.2f", highScore) + " seconds", 10, Gdx.graphics.getHeight() - 50);  // Display high score
        batch.end();

    }


    private void updateBalls() {
            if (ball1Controller != null && isBall1Visible) {
                float axisX = ball1Controller.getAxis(0); // Left stick X-axis
                float axisY = ball1Controller.getAxis(1); // Left stick Y-axis
                ball1Pos.x += axisX * 5f;  // Adjust speed multiplier as needed
                ball1Pos.y -= axisY * 5f;  // Inverted Y-axis for correct direction
            }

            if (ball2Controller != null && isBall2Visible) {
                float axisX = ball2Controller.getAxis(0); // Left stick X-axis
                float axisY = ball2Controller.getAxis(1); // Left stick Y-axis
                ball2Pos.x += axisX * 5f;
                ball2Pos.y -= axisY * 5f;
            }

            // Keep balls within screen bounds
            clampToScreen(ball1Pos);
            clampToScreen(ball2Pos);
        }

        private void updateEnemyBalls() {
            // Let each enemy ball hunt the closest player ball
            for (EnemyBall enemy : enemyBalls) {
                Vector2 target = null;

                // Check which player ball is visible and closer to the enemy ball
                if (isBall1Visible && isBall2Visible) {
                    // Find the closest ball
                    if (enemy.position.dst(ball1Pos) < enemy.position.dst(ball2Pos)) {
                        target = ball1Pos;
                    } else {
                        target = ball2Pos;
                    }
                } else if (isBall1Visible) {
                    target = ball1Pos;
                } else if (isBall2Visible) {
                    target = ball2Pos;
                }

                // Update the enemy ball to move towards the target player ball
                enemy.update(target);
            }
        }

        private void clampToScreen(Vector2 pos) {
            if (pos.x < ballRadius) pos.x = ballRadius;
            if (pos.x > Gdx.graphics.getWidth() - ballRadius) pos.x = Gdx.graphics.getWidth() - ballRadius;
            if (pos.y < ballRadius) pos.y = ballRadius;
            if (pos.y > Gdx.graphics.getHeight() - ballRadius) pos.y = Gdx.graphics.getHeight() - ballRadius;
        }

        private void checkCollision() {
            if (isBall1Visible && isBall2Visible) {
                float distance = ball1Pos.dst(ball2Pos);  // Calculate distance between the two balls
                float minDistance = ballRadius * 2;  // The minimum distance to prevent overlap (sum of the radii)

                if (distance < minDistance) {
                    // Calculate the overlap amount
                    float overlap = minDistance - distance;

                    // Calculate the direction of the separation vector
                    Vector2 separation = new Vector2(ball1Pos).sub(ball2Pos).nor();

                    // Apply the separation equally to both balls to push them apart
                    ball1Pos.add(separation.scl(overlap / 2f));
                    ball2Pos.sub(separation.scl(overlap / 2f));

                    // Re-check if the balls are out of screen bounds after adjusting for collision
                    clampToScreen(ball1Pos);
                    clampToScreen(ball2Pos);
                }
            }
        }

        private void checkEnemyCollision() {
            Iterator<EnemyBall> enemyIterator = enemyBalls.iterator();
            while (enemyIterator.hasNext()) {
                EnemyBall enemy = enemyIterator.next();

                // Check collision with ball 1
                if (isBall1Visible && ball1Pos.dst(enemy.position) < ballRadius * 2) {
                    // Player 1 is dead, hide the ball
                    isBall1Visible = false;
                    ball1Controller = null; // Optional: Remove control if you want to let it be assigned later
                    enemyIterator.remove();  // Remove the enemy ball
                    playerScore++;  // Increase player score
                    continue;  // Skip to the next enemy ball to avoid double removal
                }

                // Check collision with ball 2
                if (isBall2Visible && ball2Pos.dst(enemy.position) < ballRadius * 2) {
                    // Player 2 is dead, hide the ball
                    isBall2Visible = false;
                    ball2Controller = null; // Optional: Remove control if you want to let it be assigned later
                    enemyIterator.remove();  // Remove the enemy ball
                    playerScore++;  // Increase player score
                }
            }
        }
/*
    private void resetGame() {
        survivalTime = 0f;
        isBall1Visible = true;
        isBall2Visible = true;
        // Reset other game state as needed, such as respawning players and enemies
    }
*/
    @Override
    public void dispose() {
        shapeRenderer.dispose();
        batch.dispose();
        font.dispose();  // Dispose of the font to prevent memory leaks
    }

}
