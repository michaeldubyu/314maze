import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Random;
import java.io.FileInputStream;

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
	private static int tex_floor;
	private static int tex_wall;
	private static int tex_ceiling;
	private static int tex_painting;
	
	private static int[][] walls = new int[50][50];
	private static int playerX;
	private static int playerY;
	private static int wallRecurs;
	private static Random r;
	private static Random r2;
	
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
		//initialize the walls
		for (int i=0;i<40;i++){
			for (int k=0;k<40;k++){
				walls[i][k] = 0;
			}
		} 
		wallRecurs = 0;
		Random r = new Random();
		int nextSplit=r.nextInt(40);		
		generateMaze(nextSplit,true);
		
		playerX = 20;
		playerY = 20;
		
		/*for (int i=0;i<150;i++){
			for (int k=0;k<150;k++){
				System.out.print(walls[i][k]);
			}
			System.out.print("\n");
		}*/
		
	    tex_floor = setupTextures("res/textures/ground.png");
	    tex_ceiling = setupTextures("res/textures/ceiling.png");
	    tex_wall = setupTextures("res/textures/brick.png");
	    tex_painting = setupTextures("res/textures/picture.png");

		matSpecular.put(1.0f).put(1.0f).put(1.0f).put(1.0f).flip();
		matAmbientAndDiffuse.put(0.8f).put(0.8f).put(0.8f).put(1.0f).flip();
		
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
	
	public static void drawCeiling() {
		glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
		glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
		
        glEnable(GL_TEXTURE_2D);
	    glBindTexture(GL_TEXTURE_2D, tex_ceiling);
	    glMaterial(GL_FRONT_AND_BACK, GL_SPECULAR, matSpecular);
	    glMaterialf(GL_FRONT_AND_BACK, GL_SHININESS, 100.0f);
	    glMaterial(GL_FRONT_AND_BACK, GL_AMBIENT_AND_DIFFUSE, matAmbientAndDiffuse);
	    glBegin(GL_QUADS);
    		glTexCoord2d(0,0);
    		glNormal3f(0,1.0f,0);
    		glVertex3f(-100,2.5f,-100);
	    
	    	glTexCoord2d(0,100);
	    	glNormal3f(0,1.0f,0);
	    	glVertex3f(-100,2.5f,100);
	    	
	    	glTexCoord2d(100,100);
	    	glNormal3f(0,1.0f,0);
	    	glVertex3f(100,2.5f,100);
	    	
	    	glTexCoord2d(100,0);	 
	    	glNormal3f(0,1.0f,0);
	    	glVertex3f(100,2.5f,-100);
	    glEnd();
	    glDisable(GL_TEXTURE_2D);
	}
	
	public static void drawWall(int tex) {
		glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
		glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);

        glEnable(GL_TEXTURE_2D);
	    glBindTexture(GL_TEXTURE_2D, tex);
	    glMaterial(GL_FRONT_AND_BACK, GL_SPECULAR, matSpecular);
	    glMaterialf(GL_FRONT_AND_BACK, GL_SHININESS, 100.0f);
	    glMaterial(GL_FRONT_AND_BACK, GL_AMBIENT_AND_DIFFUSE, matAmbientAndDiffuse);
	    glBegin(GL_QUADS);
    		glTexCoord2d(0,0);
    		glNormal3f(0,1.0f,0);
    		glVertex3f(-2.5f,-2.5f,0);
	    
	    	glTexCoord2d(0,2);
	    	glNormal3f(0,1.0f,0);
	    	glVertex3f(-2.5f,2.5f,0);
	    	
	    	glTexCoord2d(2,2);
	    	glNormal3f(0,1.0f,0);
	    	glVertex3f(2.5f,2.5f,0);
	    	
	    	glTexCoord2d(2,0);	 
	    	glNormal3f(0,1.0f,0);
	    	glVertex3f(2.5f,-2.5f, 0);
	    glEnd();
	    glDisable(GL_TEXTURE_2D);
	}
	
	public static void drawFloor() {
		glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
		glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);		
        glEnable(GL_TEXTURE_2D);
	    glBindTexture(GL_TEXTURE_2D, tex_floor);
	    glMaterial(GL_FRONT_AND_BACK, GL_SPECULAR, matSpecular);
	    glMaterialf(GL_FRONT_AND_BACK, GL_SHININESS, 100.0f);
	    glMaterial(GL_FRONT_AND_BACK, GL_AMBIENT_AND_DIFFUSE, matAmbientAndDiffuse);
	    glBegin(GL_QUADS);
    		glTexCoord2d(0,0);
    		glNormal3f(0,1.0f,0);
    		glVertex3f(-100,-2.5f,-100);
	    
	    	glTexCoord2d(0,100);
	    	glNormal3f(0,1.0f,0);
	    	glVertex3f(-100,-2.5f,100);
	    	
	    	glTexCoord2d(100,100);
	    	glNormal3f(0,1.0f,0);
	    	glVertex3f(100,-2.5f,100);
	    	
	    	glTexCoord2d(100,0);	 
	    	glNormal3f(0,1.0f,0);
	    	glVertex3f(100,-2.5f,-100);
	    glEnd();
	    glDisable(GL_TEXTURE_2D);
	}
	
	public static void drawMazeWalls(){
		//far back wall
		glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
		glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);		
        glEnable(GL_TEXTURE_2D);
	    glBindTexture(GL_TEXTURE_2D, tex_wall);
	    glMaterial(GL_FRONT_AND_BACK, GL_SPECULAR, matSpecular);
	    glMaterialf(GL_FRONT_AND_BACK, GL_SHININESS, 100.0f);
	    glMaterial(GL_FRONT_AND_BACK, GL_AMBIENT_AND_DIFFUSE, matAmbientAndDiffuse);
	    glBegin(GL_QUADS);
    		glTexCoord2d(0,0);
    		glNormal3f(0,1.0f,0);
    		glVertex3f(-100,-2.5f,-100);
	    
	    	glTexCoord2d(0,2);
	    	glNormal3f(0,1.0f,0);
	    	glVertex3f(-100, 2.5f,-100);
	    	
	    	glTexCoord2d(50,2);
	    	glNormal3f(0,1.0f,0);
	    	glVertex3f(100,2.5f,-100);
	    	
	    	glTexCoord2d(50,0);	 
	    	glNormal3f(0,1.0f,0);
	    	glVertex3f(100,-2.5f,-100);
	    glEnd();
	    
	    //right side of player (when they spawn)		
	    glBegin(GL_QUADS);
    		glTexCoord2d(0,0);
    		glNormal3f(0,1.0f,0);
    		glVertex3f(100,-2.5f,-100);
	    
	    	glTexCoord2d(0,2);
	    	glNormal3f(0,1.0f,0);
	    	glVertex3f(100, 2.5f,-100);
	    	
	    	glTexCoord2d(50,2);
	    	glNormal3f(0,1.0f,0);
	    	glVertex3f(100, 2.5f, 100);
	    	
	    	glTexCoord2d(50,0);	 
	    	glNormal3f(0,1.0f,0);
	    	glVertex3f(100,-2.5f, 100);
	    glEnd();
	    
	    //back face of maze	
	    glBegin(GL_QUADS);
    		glTexCoord2d(0,0);
    		glNormal3f(0,1.0f,0);
    		glVertex3f(100,-2.5f, 100);
	    
	    	glTexCoord2d(0,2);
	    	glNormal3f(0,1.0f,0);
	    	glVertex3f(100, 2.5f, 100);
	    	
	    	glTexCoord2d(50,2);
	    	glNormal3f(0,1.0f,0);
	    	glVertex3f(-100, 2.5f, 100);
	    	
	    	glTexCoord2d(50,0);	 
	    	glNormal3f(0,1.0f,0);
	    	glVertex3f(-100,-2.5f, 100);
	    glEnd();
	    
	    //left face of maze
	    glBegin(GL_QUADS);
    		glTexCoord2d(0,0);
    		glNormal3f(0,1.0f,0);
    		glVertex3f(-100,-2.5f, 100);
	    
	    	glTexCoord2d(0,2);
	    	glNormal3f(0,1.0f,0);
	    	glVertex3f(-100, 2.5f, 100);
	    	
	    	glTexCoord2d(50,2);
	    	glNormal3f(0,1.0f,0);
	    	glVertex3f(-100, 2.5f, -100);
	    	
	    	glTexCoord2d(50,0);	 
	    	glNormal3f(0,1.0f,0);
	    	glVertex3f(-100,-2.5f, -100);
	    glEnd();
	    
	}
	
	
	public static void generateMaze(int splitPoint, boolean vertical){		
		//use recursive division
		//we'll represent a wall with 1, and no wall with 0
		if (wallRecurs == 100) return;
		wallRecurs++;
		int split = (int)(splitPoint % (40));
		r = new Random();
		int randomOpening = r.nextInt(35);
		boolean vert = vertical;
		if (vertical){
			//splitting vertically
			for (int k=0;k<split;k++) walls[split][k] = 1;
			walls[split][randomOpening] = 0;
			walls[split][randomOpening+1] = 0;
			walls[split][randomOpening+2] = 0;
			walls[split][randomOpening+3] = 0;
			vertical = false;
		}
		else{
			//splitting horizontally
			for (int k=0;k<split;k++) walls[k][split] = 1;
			walls[randomOpening][split] = 0;
			walls[randomOpening+1][split] = 0;
			walls[randomOpening+2][split] = 0;
			walls[randomOpening+3][split] = 0;
			vertical = true;
		}
		
		r2 = new Random();
		int nextSplit = r.nextInt(50);		
		generateMaze(nextSplit,vertical);

	}
   
	public void drawMaze(){
		for (int i=0;i<40;i++){
			glPushMatrix();
			for (int k=0;k<40;k++){
				if (walls[i][k]==1){
					drawWall(tex_wall);
				}
				glTranslatef(0,0,5f);
			}
			glPopMatrix();
			glTranslatef(5,0,0);
		}
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
  
        	//we are in the first person view
        	matrixData = BufferUtils.createFloatBuffer(16);
        	increPlayerMatrix.store(matrixData);
        	matrixData.flip();
        	glMultMatrix(matrixData);
        	
        	matrixData = BufferUtils.createFloatBuffer(16);
        	playerMatrix.store(matrixData);
        	matrixData.flip();

        	//save player matrix
        	glMultMatrix(matrixData);
        	glGetFloat(GL_MODELVIEW_MATRIX, matrixData);
        	playerMatrix.load(matrixData);
        	matrixData.flip();
        
        	glPushMatrix();
        	drawMazeWalls();
        	glPopMatrix();
        	
        	glPushMatrix();
        	glTranslatef(-100,0,-100);
        	drawMaze();
        	glPopMatrix();
        	
			//draw floor
	        glPushMatrix();
	        drawFloor();
	        glPopMatrix();
	        
	        //draw ceiling
	        glPushMatrix();
	        drawCeiling();
	        glPopMatrix();
	        
	        pollInput();
	        
		    Display.update();
		    Display.sync(60);
		    updateFPS();
        }

        Display.destroy();
    }
    
    public boolean isColliding(int x, int y){
    	return walls[x][y] == 1 ? true : false;
    }
    
    public void pollInput() {
    	System.out.println(playerX + " " + playerY + " " + isColliding(playerX, playerY));
    	System.out.println(playerX+1 + " " + playerY+1 + " " + isColliding(playerX+1, playerY+1));
    	System.out.println();
    	glLoadIdentity();
    	if (Keyboard.isKeyDown(Keyboard.KEY_A)) {
    		if (!isColliding(playerX+1, playerY)) {
    			glTranslatef(distX,0,0);
    			playerX += 1;
    		}
        }
    	else if (Keyboard.isKeyDown(Keyboard.KEY_D)) {
    		if (!isColliding(playerX-1, playerY)) {
    			glTranslatef(-distX,0,0);
    			playerX -= 1;
    		}        
    	}
    	else if (Keyboard.isKeyDown(Keyboard.KEY_W)) {
    		if (!isColliding(playerX, playerY+1)) {
    			glTranslatef(0,0,distZ);
    			playerY += 1;
    		}      
    	}
    	else if (Keyboard.isKeyDown(Keyboard.KEY_S)) {
    		if (!isColliding(playerX, playerY-1)) {
    			glTranslatef(0,0,distZ);
    			playerY -= 1;
    		}        
    	}
    	else if (Keyboard.isKeyDown(Keyboard.KEY_Q)) {
    		glRotatef(-5,0,1,0);
        }
    	else if (Keyboard.isKeyDown(Keyboard.KEY_E)) {
    		glRotatef(5,0,1,0);
        }

    	matrixData = BufferUtils.createFloatBuffer(16);
    	glGetFloat(GL_MODELVIEW_MATRIX, matrixData);
    	increPlayerMatrix.load(matrixData);
    }
    
    public void updateFPS() {
        if (getTime() - lastFPS > 1000) {
            Display.setTitle("Windows 95 Screensaver - FPS: " + fps); 
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