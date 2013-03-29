import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.LWJGLException;
import org.lwjgl.Sys;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import org.lwjgl.util.vector.Matrix4f;

import de.matthiasmann.twl.utils.PNGDecoder;
import static org.lwjgl.util.glu.GLU.*;
import static org.lwjgl.opengl.GL11.*;

public class PuzzleGame{

	private static boolean animating = false;
	private static int tex;
	
	int fps;
	long lastFPS;
	
    static FloatBuffer matSpecular = BufferUtils.createFloatBuffer(4);
    static FloatBuffer matAmbientAndDiffuse = BufferUtils.createFloatBuffer(4);
    
    static Matrix4f playerMatrix = new Matrix4f();
    static Matrix4f increPlayerMatrix = new Matrix4f();
    private static FloatBuffer matrixData;


	static float scale = 1.0f;
	static float spacing = 2.5f * scale;
	static float distX = 1;
	static float distZ = 1;
	static float distY = 5;
	static float angleX = 0;
	static float angleY = 0;
	static int cameraMode = 1; //by default we are in overhead view

	public static void init(){
	    
	    tex = setupTextures("res/textures/table.png");
	    
		matSpecular.put(1.0f).put(1.0f).put(1.0f).put(1.0f).flip();
		matAmbientAndDiffuse.put(0.6f).put(0.6f).put(0.8f).put(1.0f).flip();
		
		FloatBuffer lightSpecular = BufferUtils.createFloatBuffer(4);
		FloatBuffer lightDiffuse = BufferUtils.createFloatBuffer(4);
		FloatBuffer lightAmbient = BufferUtils.createFloatBuffer(4);
		
	    lightSpecular.put(0.4f).put(0.4f).put(0.4f).put(1.0f).flip();
	    lightDiffuse.put(0.9f).put(0.9f).put(0.9f).put(1.0f).flip();
	    lightAmbient.put(0.2f).put(0.2f).put(0.2f).put(1.0f).flip();

	    FloatBuffer lightPosition = BufferUtils.createFloatBuffer(4);
	    lightPosition.put(-10.0f).put(0.0f).put(0.0f).put(10.0f).flip();
	    
	    FloatBuffer ambient = BufferUtils.createFloatBuffer(4);
		ambient.put(0.8f).put(0.8f).put(0.8f).put(1.0f).flip();

	    glClearColor (0.0f, 0.0f, 0.0f, 0.0f);
	    
	    glEnable(GL_LIGHTING);
	    glEnable(GL_LIGHT0);
	    
	    glLight(GL_LIGHT0, GL_AMBIENT, lightAmbient);
	    glLight(GL_LIGHT0, GL_SPECULAR, lightSpecular);
	    glLight(GL_LIGHT0, GL_DIFFUSE, lightDiffuse);

	    glLight(GL_LIGHT0, GL_POSITION, lightPosition);

	    glLightModel(GL_LIGHT_MODEL_AMBIENT, ambient);
	    glLightModeli(GL_LIGHT_MODEL_TWO_SIDE, GL_TRUE);
	    
	}

	
	private static int setupTextures(String filename) {
	    IntBuffer tmp = BufferUtils.createIntBuffer(1);
	    glGenTextures(tmp);
	    tmp.rewind();
	    try {
	        InputStream in = new FileInputStream(filename);
	        PNGDecoder decoder = new PNGDecoder(in);

	        ByteBuffer buf = ByteBuffer.allocateDirect(4 * decoder.getWidth() * decoder.getHeight());
	        decoder.decode(buf, decoder.getWidth() * 4, PNGDecoder.Format.RGBA);
	        buf.flip();

	        glBindTexture(GL_TEXTURE_2D, tmp.get(0));
	        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER,
	                GL_NEAREST);
	        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER,
	                GL_NEAREST);
	        glPixelStorei(GL_UNPACK_ALIGNMENT, 4);
	        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, decoder.getWidth(), decoder.getHeight(), 0, GL_RGBA, GL_UNSIGNED_BYTE, buf);
	        int unsigned = (buf.get(0) & 0xff);

	    } catch (java.io.FileNotFoundException ex) {
	        System.out.println("Error " + filename + " not found");
	    } catch (java.io.IOException e) {
	        System.out.println("Error decoding " + filename);
	    }
	    tmp.rewind();
	    return tmp.get(0);
	}
	
	public static void drawFloor() {
        glEnable(GL_TEXTURE_2D);
	    glBindTexture(GL_TEXTURE_2D, tex);
	    glMaterial(GL_FRONT_AND_BACK, GL_SPECULAR, matSpecular);
	    glMaterialf(GL_FRONT_AND_BACK, GL_SHININESS, 100.0f);
	    glMaterial(GL_FRONT_AND_BACK, GL_AMBIENT_AND_DIFFUSE, matAmbientAndDiffuse);
	    glBegin(GL_QUADS);
    		glTexCoord2d(0,0);
    		glNormal3f(0,1.0f,0);
    		glVertex3f(-500,-10,-500);
	    
	    	glTexCoord2d(0,1);
	    	glNormal3f(0,1.0f,0);
	    	glVertex3f(-500,-20,500);
	    	
	    	glTexCoord2d(1,1);
	    	glNormal3f(0,1.0f,0);
	    	glVertex3f(500,-20,500);
	    	
	    	glTexCoord2d(1,0);	 
	    	glNormal3f(0,1.0f,0);
	    	glVertex3f(500,-20,-500);
	    glEnd();
	    glDisable(GL_TEXTURE_2D);
	}
	
    
    public static void drawQuad(float scale){
    	
    	glPushMatrix();
        glBegin(GL_QUADS);        
        	glColor3f(0.5f,0.8f,0.8f);
        	glMaterial(GL_FRONT_AND_BACK, GL_SPECULAR, matSpecular);
		    glMaterialf(GL_FRONT_AND_BACK, GL_SHININESS, 50.0f);
		    glMaterial(GL_FRONT_AND_BACK, GL_AMBIENT_AND_DIFFUSE, matAmbientAndDiffuse);
	        glVertex3f(1.0f * scale, 1.0f * scale, 1.0f * scale);   // draw top right
	        glVertex3f(-1.0f * scale, 1.0f * scale, 1.0f * scale);  // Top Left Of The Quad (Top)
	        glVertex3f(-1.0f * scale, -1.0f * scale, 1.0f * scale);   // Bottom Left Of The Quad (Top)
	        glVertex3f(1.0f * scale, -1.0f * scale, 1.0f * scale);    // Bottom Right Of The Quad (Top)
        glEnd();
        glPopMatrix();
    }
	
    public void idle(){

    }
    
    public void start() {
        try {
		    Display.setDisplayMode(new DisplayMode(800, 600));
		    Display.create();
		} catch (LWJGLException e) {
		    e.printStackTrace();
		    System.exit(0);
		}

        init();
        glShadeModel(GL_SMOOTH);              
        glClearColor(0.0f, 0.0f, 0.0f, 0.5f);    
        glClearDepth(1.0f);                      
        glEnable(GL_DEPTH_TEST);              
        glDepthFunc(GL_LEQUAL);    
	    //glEnable(GL_COLOR_MATERIAL);
        glHint(GL_PERSPECTIVE_CORRECTION_HINT, GL_NICEST);  
 
        lastFPS = getTime(); //init FPS time

        while (!Display.isCloseRequested()) {

		    // Clear the screen and depth buffer
		    glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);	
			
		    // set the color of the quad (R,G,B,A)
		    glMatrixMode(GL_PROJECTION);
	        
		    // set up view here
		    glLoadIdentity();
		    gluPerspective(60, 1, 1, 1500 ); //near of 1, far of 100
		    glMatrixMode(GL_MODELVIEW);
	        glLoadIdentity();	
	        	        
	        if (cameraMode == 0){
	        	//we are in the first person view
	        	matrixData = BufferUtils.createFloatBuffer(16);
	        	increPlayerMatrix.store(matrixData);
	        	matrixData.flip();
	        	glMultMatrix(matrixData);
	        	
	        	matrixData = BufferUtils.createFloatBuffer(16);
	        	playerMatrix.store(matrixData);
	        	matrixData.flip();

	        	glMultMatrix(matrixData);
	        	glGetFloat(GL_MODELVIEW_MATRIX, matrixData);
	        	playerMatrix.load(matrixData);
	        	matrixData.flip();
	        }
			//draw table
	        glPushMatrix();
	        drawFloor();
	        glPopMatrix();
	        pollInput();
	        
		    Display.update();
		    Display.sync(60);
		    updateFPS();
		    
            idle(); //perform animation calculations
        }

        Display.destroy();
    }
    
    public void pollInput() {
    	glLoadIdentity();
    	if (Keyboard.isKeyDown(Keyboard.KEY_A)) {
    		glTranslatef(distX,0,0);
        }
    	else if (Keyboard.isKeyDown(Keyboard.KEY_D)) {
    		glTranslatef(-distX,0,0);
        }
    	else if (Keyboard.isKeyDown(Keyboard.KEY_W)) {
    		glTranslatef(0,0,distZ);
        }
    	else if (Keyboard.isKeyDown(Keyboard.KEY_S)) {
    		glTranslatef(0,0,-distZ);
        }
    	else if (Keyboard.isKeyDown(Keyboard.KEY_Q)) {
    		glRotatef(-1,0,1,0);
        }
    	else if (Keyboard.isKeyDown(Keyboard.KEY_E)) {
    		glRotatef(1,0,1,0);
        }
    	else if (Keyboard.isKeyDown(Keyboard.KEY_F)) {
        	if (cameraMode == 1) cameraMode = 0;
        	else if (cameraMode == 0) cameraMode = 1;
        }
    	matrixData = BufferUtils.createFloatBuffer(16);
    	glGetFloat(GL_MODELVIEW_MATRIX, matrixData);
    	increPlayerMatrix.load(matrixData);
    }
    
    public void updateFPS() {
        if (getTime() - lastFPS > 1000) {
            Display.setTitle("Rubrick 9000 - FPS: " + fps); 
            fps = 0; //reset the FPS counter
            lastFPS += 1000; //add one second
        }
        fps++;
    }
    
    public long getTime(){
    	return (Sys.getTime() * 1000) / Sys.getTimerResolution();
    }
    
    public static void main(String[] argv) {
    	PuzzleGame Game = new PuzzleGame();
    	Game.start();
    }
}