package com.monstrous.testSponza;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Version;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g3d.shaders.DepthShader;
import com.badlogic.gdx.graphics.g3d.utils.CameraInputController;
import com.badlogic.gdx.math.Vector3;
import net.mgsx.gltf.loaders.gltf.GLTFLoader;
import net.mgsx.gltf.scene3d.attributes.PBRCubemapAttribute;
import net.mgsx.gltf.scene3d.attributes.PBRTextureAttribute;
import net.mgsx.gltf.scene3d.lights.DirectionalLightEx;
import net.mgsx.gltf.scene3d.lights.DirectionalShadowLight;
import net.mgsx.gltf.scene3d.scene.*;
import net.mgsx.gltf.scene3d.shaders.PBRShaderConfig;
import net.mgsx.gltf.scene3d.shaders.PBRShaderProvider;
import net.mgsx.gltf.scene3d.utils.IBLBuilder;
import static com.badlogic.gdx.graphics.GL20.GL_VERSION;

import static net.mgsx.gltf.scene3d.shaders.PBRShaderProvider.createDefaultConfig;

public class Main extends ApplicationAdapter
{
    private SceneManager sceneManager;
    private SceneAsset sceneAsset;
    private Scene scene;
    private PerspectiveCamera camera;
    private Cubemap diffuseCubemap;
    private Cubemap environmentCubemap;
    private Cubemap specularCubemap;
    private Texture brdfLUT;
    private float time;
    private SceneSkybox skybox;
    private DirectionalLightEx light;
    private DirectionalShadowLight shadowLight;
    private DirectionalLightEx sun;
    private CameraInputController camController;
    private BitmapFont font;
    private SpriteBatch batch;
    private CascadeShadowMap csm;
    private boolean withShadows = true;

    @Override
    public void create() {
        Gdx.app.log("LibGDX version: ", Version.VERSION);
        Gdx.app.log("GL version: ", Gdx.gl.glGetString(GL_VERSION));
        Gdx.app.log("GLES 3.2: ", Gdx.gl32 == null? "N/A" : "Available");
        Gdx.app.log("GLES 3.1: ", Gdx.gl31 == null? "N/A" : "Available");
        Gdx.app.log("GLES 3.0: ", Gdx.gl30 == null? "N/A" : "Available");
        Gdx.app.log("GLES 2.0: ", Gdx.gl20 == null? "N/A" : "Available");


        PBRShaderConfig config = createDefaultConfig();
        config.numBones = 0;
        config.numDirectionalLights = 1;
        config.numPointLights = 0;
        config.numSpotLights = 0;

        DepthShader.Config depthConfig= new DepthShader.Config();
        depthConfig.numBones = 0;
        sceneManager = new SceneManager(PBRShaderProvider.createDefault(config), PBRShaderProvider.createDefaultDepth(depthConfig));

        // create scene
        sceneAsset = new GLTFLoader().load(Gdx.files.internal("sponza/Sponza.gltf"));
        scene = new Scene(sceneAsset.scene);

        sceneManager.addScene(scene);

        // setup camera
        camera = new PerspectiveCamera(120f, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.near = 0.1f;
        camera.far = 100f;
        camera.position.set(5.7f, 5, -1.6f);
        camera.lookAt(new Vector3(-10,5,0));
        camera.update();

        System.out.println("camera dir:"+camera.direction);
        sceneManager.setCamera(camera);

        if(withShadows) {
            csm = new CascadeShadowMap(2);
            sceneManager.setCascadeShadowMap(csm);

            // setup light
            shadowLight = new DirectionalShadowLight();
            shadowLight.setShadowMapSize(4096, 4096);
            shadowLight.setCenter(camera.position);
            sun = shadowLight;
        } else {
            light = new DirectionalLightEx();
            sun = light;
        }
        sun.direction.set(-1, -3, 0.5f).nor();
        sun.color.set(Color.WHITE);
        sun.intensity = 4;
        sceneManager.environment.add(sun);

        // setup quick IBL (image based lighting)
        IBLBuilder iblBuilder = IBLBuilder.createOutdoor(sun);
        environmentCubemap = iblBuilder.buildEnvMap(1024);
        diffuseCubemap = iblBuilder.buildIrradianceMap(256);
        specularCubemap = iblBuilder.buildRadianceMap(10);
        iblBuilder.dispose();

        // This texture is provided by the library, no need to have it in your assets.
        brdfLUT = new Texture(Gdx.files.classpath("net/mgsx/gltf/shaders/brdfLUT.png"));

        sceneManager.setAmbientLight(0.2f);
        sceneManager.environment.set(new PBRTextureAttribute(PBRTextureAttribute.BRDFLUTTexture, brdfLUT));
        sceneManager.environment.set(PBRCubemapAttribute.createSpecularEnv(specularCubemap));
        sceneManager.environment.set(PBRCubemapAttribute.createDiffuseEnv(diffuseCubemap));

        // setup skybox
        skybox = new SceneSkybox(environmentCubemap);
        sceneManager.setSkyBox(skybox);

        camController = new CameraInputController(camera);
        Gdx.input.setInputProcessor(camController);

        font = new BitmapFont();
        batch = new SpriteBatch();
    }

    @Override
    public void resize(int width, int height) {
        sceneManager.updateViewport(width, height);
        batch.getProjectionMatrix().setToOrtho2D(0,0,width, height);
    }

    @Override
    public void render() {
        float deltaTime = Gdx.graphics.getDeltaTime();
        time += deltaTime;

        camController.update();

        if(withShadows) {
            DirectionalShadowLight shadowLight = sceneManager.getFirstDirectionalShadowLight();
            csm.setCascades(sceneManager.camera, shadowLight, 32f, 4f);
        }
        // render
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
        sceneManager.update(deltaTime);
        sceneManager.render();

        batch.begin();
        font.draw(batch, "cam pos: " +camera.position.toString(), 10, 100);
        font.draw(batch, "FPS: " +Gdx.graphics.getFramesPerSecond(), 10, 50);
        batch.end();
    }

    @Override
    public void dispose() {
        sceneManager.dispose();
        sceneAsset.dispose();
        environmentCubemap.dispose();
        diffuseCubemap.dispose();
        specularCubemap.dispose();
        brdfLUT.dispose();
        skybox.dispose();
        batch.dispose();
        font.dispose();
    }
}
