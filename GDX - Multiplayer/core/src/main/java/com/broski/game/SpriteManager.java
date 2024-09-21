package com.broski.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Texture;

public class SpriteManager {

    public static Texture backgroundTexture;
    public static Texture bucketTexture;
    public static Texture dropTexture;
    public static Sound dropSound;
    public static Music music;

    // Static initializer or load method
    static {
        loadSprites();
    }

    public static void loadSprites() {
        backgroundTexture = new Texture("background.png");
        bucketTexture = new Texture("bucket.png");
        dropTexture = new Texture("drop.png");
        dropSound = Gdx.audio.newSound(Gdx.files.internal("drop.mp3"));
        music = Gdx.audio.newMusic(Gdx.files.internal("music.mp3"));

    }


}
