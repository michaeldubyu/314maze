import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Random;
import java.io.FileInputStream;

import org.lwjgl.BufferUtils;
import org.lwjgl.LWJGLException;
import org.lwjgl.Sys;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import org.lwjgl.util.Timer;
import org.lwjgl.util.vector.Matrix;
import org.lwjgl.util.vector.Matrix4f;

import de.matthiasmann.twl.utils.PNGDecoder;
import static org.lwjgl.util.glu.GLU.*;
import static org.lwjgl.opengl.GL11.*;

public class PuzzleGame{

	private static int tex_floor;
	private static int tex_wall;
	private static int tex_ceiling;
	private static int tex_painting;
	private static int tex_rat;
	
	private static int MAZE_WIDTH = 12;
	private static int MAZE_HEIGHT = 12;

	//new algorithm and structure for collisions and walls :
	//0 for no walls, 1 for walls, draw a 5x5x5x5 square where they exist
	private static int[][] walls = new int[MAZE_WIDTH][MAZE_HEIGHT];
	private static int playerX;
	private static int playerY;
	private static int lookAtX;
	private static int lookAtY;
	private static double playerRadius = 1;
	
	private static int wallRecurs;
	private static Rat rat;
		
	int fps;
	long lastFPS;
	static boolean gameComplete;
	static String deltaTime;
	
    static FloatBuffer matSpecular = BufferUtils.createFloatBuffer(4);
    static FloatBuffer matAmbientAndDiffuse = BufferUtils.createFloatBuffer(4);
    
    static ArrayList<String> scores = new ArrayList();
    
    static Matrix4f playerMatrix = new Matrix4f();
    static Matrix playerInvertedMatrix = new Matrix4f();
    static Matrix4f increPlayerMatrix = new Matrix4f();
    private static FloatBuffer matrixData;

	static int distX = 3;
	static int distZ = 3;

	public static void init(){
		gameComplete = false;
		wallRecurs = 0;	
		generateMaze(40,40,false);
		
		Random r = new Random();
		rat = new Rat(-r.nextInt(70), -r.nextInt(70), 2);
		playerX = 0;
		playerY = 0;		
		
	    tex_floor = setupTextures("res/textures/ground.png");
	    tex_ceiling = setupTextures("res/textures/ceiling.png");
	    tex_wall = setupTextures("res/textures/brick.png");
	    tex_painting = setupTextures("res/textures/picture.png");
	    tex_rat = setupTextures("res/textures/rat.png");

		matSpecular.put(1.0f).put(1.0f).put(1.0f).put(1.0f).flip();
		matAmbientAndDiffuse.put(0.5f).put(0.5f).put(0.5f).put(1.0f).flip();
		
		FloatBuffer lightSpecular = BufferUtils.createFloatBuffer(4);
		FloatBuffer lightDiffuse = BufferUtils.createFloatBuffer(4);
		FloatBuffer lightAmbient = BufferUtils.createFloatBuffer(4);
		
	    lightSpecular.put(1.0f).put(1.0f).put(1.0f).put(1.0f).flip();
	    lightDiffuse.put(0.1f).put(0.1f).put(0.5f).put(1.0f).flip();
	    lightAmbient.put(0.5f).put(0.5f).put(0.5f).put(1.0f).flip();

	    FloatBuffer lightPosition = BufferUtils.createFloatBuffer(4);
	    lightPosition.put(-40.0f).put(2.0f).put(0.0f).put(1.0f).flip();
	    
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
	    glMaterialf(GL_FRONT_AND_BACK, GL_SHININESS, 80.0f);
	    glMaterial(GL_FRONT_AND_BACK, GL_AMBIENT_AND_DIFFUSE, matAmbientAndDiffuse);
	    glBegin(GL_QUADS);
    		glTexCoord2d(0,0);
    		glNormal3f(0,-1.0f,0);
    		glVertex3f(-80,2.5f,-80);
	    
	    	glTexCoord2d(0,20);
	    	glNormal3f(0,-1.0f,0);
	    	glVertex3f(0,2.5f,-80);
	    	
	    	glTexCoord2d(20,20);
	    	glNormal3f(0,-1.0f,0);
	    	glVertex3f(0,2.5f,0);
	    	
	    	glTexCoord2d(20,0);	 
	    	glNormal3f(0,-1.0f,0);
	    	glVertex3f(-80,2.5f,0);
	    glEnd();
	    glDisable(GL_TEXTURE_2D);
	}
	
	public static void drawWall(int tex) {
		glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
		glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);

        glEnable(GL_TEXTURE_2D);
	    glBindTexture(GL_TEXTURE_2D, tex);
	    glMaterial(GL_FRONT_AND_BACK, GL_SPECULAR, matSpecular);
	    glMaterialf(GL_FRONT_AND_BACK, GL_SHININESS, 80.0f);
	    glMaterial(GL_FRONT_AND_BACK, GL_AMBIENT_AND_DIFFUSE, matAmbientAndDiffuse);
	    glBegin(GL_QUADS);
    		glTexCoord2d(0,0);
    		glNormal3f(0,0.1f,0.5f);
    		glVertex3f(-2.5f,-2.5f,0);
	    
    		if (tex!=tex_painting)glTexCoord2d(0,2);
    		else glTexCoord2d(0,-1);
    		glNormal3f(0,0,1.0f);
	    	glVertex3f(-2.5f,2.5f,0);
	    	
	    	if (tex!=tex_painting)glTexCoord2d(2,2);
	    	else glTexCoord2d(-1,-1);
    		glNormal3f(0,0,1.0f);
	    	glVertex3f(2.5f,2.5f,0);
	    	
	    	if (tex!=tex_painting)glTexCoord2d(2,0);	
	    	else glTexCoord2d(-1,0);
    		glNormal3f(0,0,1.0f);
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
	    glMaterialf(GL_FRONT_AND_BACK, GL_SHININESS, 80.0f);
	    glMaterial(GL_FRONT_AND_BACK, GL_AMBIENT_AND_DIFFUSE, matAmbientAndDiffuse);
	    glBegin(GL_QUADS);
			glTexCoord2d(0,0);
			glNormal3f(0,1.0f,0);
			glVertex3f(-80,-2.5f,-80);
	    
	    	glTexCoord2d(0,80);
	    	glNormal3f(0,1.0f,0);
	    	glVertex3f(0,-2.5f,-80);
	    	
	    	glTexCoord2d(80,80);
	    	glNormal3f(0,1.0f,0);
	    	glVertex3f(0,-2.5f,0);
	    	
	    	glTexCoord2d(80,0);	 
	    	glNormal3f(0,1.0f,0);
	    	glVertex3f(-80,-2.5f,0);
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
	    glMaterialf(GL_FRONT_AND_BACK, GL_SHININESS, 80.0f);
	    glMaterial(GL_FRONT_AND_BACK, GL_AMBIENT_AND_DIFFUSE, matAmbientAndDiffuse);
	    glBegin(GL_QUADS);
    		glTexCoord2d(0,0);
    		glNormal3f(0,1.0f,0);
    		glVertex3f(-80,-2.5f,-80);
	    
	    	glTexCoord2d(0,2);
	    	glNormal3f(0,1.0f,0);
	    	glVertex3f(-80, 2.5f,-80);
	    	
	    	glTexCoord2d(50,2);
	    	glNormal3f(0,1.0f,0);
	    	glVertex3f(0,2.5f,-80);
	    	
	    	glTexCoord2d(50,0);	 
	    	glNormal3f(0,1.0f,0);
	    	glVertex3f(0,-2.5f,-80);
	    glEnd();
	    
	    //right side of player (when they spawn)		
	    glBegin(GL_QUADS);
    		glTexCoord2d(0,0);
    		glNormal3f(0,1.0f,0);
    		glVertex3f(0,-2.5f,-80);
	    
	    	glTexCoord2d(0,2);
	    	glNormal3f(0,1.0f,0);
	    	glVertex3f(0, 2.5f,-80);
	    	
	    	glTexCoord2d(50,2);
	    	glNormal3f(0,1.0f,0);
	    	glVertex3f(0, 2.5f, 0);
	    	
	    	glTexCoord2d(50,0);	 
	    	glNormal3f(0,1.0f,0);
	    	glVertex3f(0,-2.5f, 0);
	    glEnd();
	    
	    //front face of maze	
	    glBegin(GL_QUADS);
    		glTexCoord2d(0,0);
    		glNormal3f(0,1.0f,0);
    		glVertex3f(-80,-2.5f, 0);
	    
	    	glTexCoord2d(0,2);
	    	glNormal3f(0,1.0f,0);
	    	glVertex3f(-80, 2.5f, 0);
	    	
	    	glTexCoord2d(50,2);
	    	glNormal3f(0,1.0f,0);
	    	glVertex3f(0, 2.5f, 0);
	    	
	    	glTexCoord2d(50,0);	 
	    	glNormal3f(0,1.0f,0);
	    	glVertex3f(0,-2.5f, 0);
	    glEnd();
	    
	    //left face of maze
	    glBegin(GL_QUADS);
    		glTexCoord2d(0,0);
    		glNormal3f(0,1.0f,0);
    		glVertex3f(-80,-2.5f, 0);
	    
	    	glTexCoord2d(0,2);
	    	glNormal3f(0,1.0f,0);
	    	glVertex3f(-80, 2.5f, 0);
	    	
	    	glTexCoord2d(50,2);
	    	glNormal3f(0,1.0f,0);
	    	glVertex3f(-80, 2.5f, -80);
	    	
	    	glTexCoord2d(50,0);	 
	    	glNormal3f(0,1.0f,0);
	    	glVertex3f(-80,-2.5f, -80);
	    glEnd();
	    
	}
	
	
	public static void generateMaze(int height, int width, boolean horizontal){		
		//we'll represent a vertical wall with |, horizontal wall with -, and no wall with 0
		if (wallRecurs == 50) return;
		else wallRecurs++;
		
		Random r = new Random();
		Random r2 = new Random();

		if (horizontal){
			int cut = r.nextInt(MAZE_HEIGHT-1); //random place along the height to split horizontally
			for (int i=0;i<MAZE_WIDTH;i++){
				walls[cut][i] = 1;
			}
			walls[cut][r2.nextInt(MAZE_WIDTH)] = 0; //the hole in the wall guaranteed to form a passage
			walls[cut][r2.nextInt(MAZE_WIDTH)] = 0; //the hole in the wall guaranteed to form a passage
			walls[cut][r2.nextInt(MAZE_WIDTH)] = 0; //the hole in the wall guaranteed to form a passage
		}else{ //cut vertically
			int cut = r.nextInt(MAZE_WIDTH-1); //some column
			for (int i=0;i<MAZE_HEIGHT;i++){
				walls[i][cut] = 1;
			}
			walls[r2.nextInt(MAZE_HEIGHT)][cut] = 0;
			walls[r2.nextInt(MAZE_HEIGHT)][cut] = 0;
			walls[r2.nextInt(MAZE_HEIGHT)][cut] = 0;
		}
		
		generateMaze(MAZE_HEIGHT,MAZE_WIDTH,!horizontal);
	}
   
	public void drawMaze(){
		
		Random r = new Random(12312312);
		for (int i=0;i<MAZE_WIDTH;i++){
			glPushMatrix();
			for (int k=0;k<MAZE_HEIGHT;k++){
				int rand = r.nextInt(10);
				//if 1 draw the cube walls at that location
				if (walls[i][k]==1){
					glPushMatrix();
					if (rand % 10 != 0)drawWall(tex_wall);
					else drawWall(tex_painting);
					glPopMatrix();
				}
				glTranslatef(0,0,-5);
			}
			glPopMatrix();
			glTranslatef(-5,0,0);
		}
	}
	
	public void drawRat(int x, int y){
		//rat movement
		
      	//draw rat
		glPushMatrix();
		glEnable(GL_BLEND); 
        glEnable(GL_TEXTURE_2D);
	    glBindTexture(GL_TEXTURE_2D, tex_rat);
	    glTranslatef(x,0,y);
		glBegin(GL_QUADS);
			glTexCoord2d(0,0);
			glNormal3f(0,1,0);
			glVertex3f(0,0.0f,-5);
			
			glTexCoord2d(0,1);
			glNormal3f(0,1,0);
			glVertex3f(0,-2.5f,-5);
			
			glTexCoord2d(1,1);
			glNormal3f(0,1,0);
			glVertex3f(3.0f,-2.5f,-5);

			glTexCoord2d(1,0);
			glNormal3f(0,1,0);
			glVertex3f(3.0f,0.0f,-5);
		glEnd();
		glDisable(GL_BLEND);
		glPopMatrix();
	}
    
    public void start() {
        try {
		    Display.setDisplayMode(new DisplayMode(600, 600));
		    Display.create();
		} catch (LWJGLException e) {
		    e.printStackTrace();
		    System.exit(0);
		}

        long startTime = System.currentTimeMillis()/1000;
        
        init();
        glShadeModel(GL_SMOOTH);      
        glEnable(GL_COLOR_MATERIAL);
        glClearColor(0.0f, 0.0f, 0.0f, 0.5f);    
        glClearDepth(1.0f);                      
        glDepthFunc(GL_LEQUAL);    
		glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glHint(GL_PERSPECTIVE_CORRECTION_HINT, GL_NICEST);  
        lastFPS = getTime(); //init FPS time

        while (!Display.isCloseRequested()) {
        	if (!gameComplete){
			    // Clear the screen and depth buffer
			    glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);	
	        	
			    // set the color of the quad (R,G,B,A)
			    glMatrixMode(GL_PROJECTION);
		        
			    // set up view here
			    glLoadIdentity();
			    gluPerspective(60, 1, 1, 500 ); //near of 1, far of 80
			    glMatrixMode(GL_MODELVIEW);
		        glLoadIdentity();	
		        glEnable(GL_LIGHTING);
		        glEnable(GL_DEPTH_TEST);              
	
	        	//we are in the first person view
	        	matrixData = BufferUtils.createFloatBuffer(16);
	        	increPlayerMatrix.store(matrixData);
	        	matrixData.flip();
	        	glMultMatrix(matrixData);
	        	
	        	matrixData = BufferUtils.createFloatBuffer(16);
	        	playerMatrix.store(matrixData);
	        	matrixData.flip();
	        	glMultMatrix(matrixData);
	
	        	//save player matrix
	        	matrixData = BufferUtils.createFloatBuffer(16);
	        	glGetFloat(GL_MODELVIEW_MATRIX, matrixData);
	        	playerMatrix.load(matrixData);     
	        	
	        	//draw the walls of the maze
	        	glPushMatrix();	
		        glTranslatef(2.5f,0,2.5f);
	        	drawMazeWalls();
	        	glPopMatrix();
	        	
				//draw floor
		        glPushMatrix();
		        glTranslatef(2.5f,0,2.5f);
		        drawFloor();
		        glPopMatrix();
		        
		        //draw ceiling
		        glPushMatrix();
		        glTranslatef(2.5f,0,2.5f);
		        drawCeiling();
		        glPopMatrix();
	        	
		        //draw brick walls of maze
	        	glPushMatrix();
		        glTranslatef(-10f,0,-5f);
	        	drawMaze();
	        	glPopMatrix();
	        	
	        	//draw rat
	        	glPushMatrix();
	        	drawRat(rat.getX(), rat.getY());
	        	glPopMatrix();
	        	
		        pollInput();
		        	        
			    updateFPS();
	
			    //2d init and drawing
			    glMatrixMode(GL_PROJECTION);
			    glLoadIdentity();
			    glOrtho(0,600,0,600,-1,1);
			    
			    glMatrixMode(GL_MODELVIEW);
			    glLoadIdentity();
	
			    glScalef(1.1f,1.1f,0);
			    glColor3f(1,1,1);
			    glDisable(GL_TEXTURE_2D);
			    glDisable(GL_DEPTH_TEST);
			    glDisable(GL_BLEND);
			    glEnable(GL_COLOR_MATERIAL);
			    
			    deltaTime = String.valueOf(System.currentTimeMillis()/1000 - startTime);
			    
			    SimpleText.drawString("THE RAT IS AT " + rat.getX() + " " + rat.getY(), 380,30);
			    SimpleText.drawString("YOU ARE AT " + playerX + " " + playerY, 400,20);
			    SimpleText.drawString("TIME ELAPSED : " + deltaTime, 400,10);
			    
			    glDisable(GL_COLOR_MATERIAL);
			    
			    Display.update();
	        }else{
	        	
			    glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);	

	        	//game complete, draw win screen				
			    //2d init and drawing	        	
			    glMatrixMode(GL_PROJECTION);
			    glLoadIdentity();
			    glOrtho(0,600,0,600,-1,1);
			    
			    glMatrixMode(GL_MODELVIEW);
			    glLoadIdentity();
			    glDisable(GL_DEPTH_TEST);
	
			    glScalef(1.1f,1.1f,0);
			    glColor3f(1,1,1);
			    glDisable(GL_TEXTURE_2D);
			    glEnable(GL_COLOR_MATERIAL);
			    glDisable(GL_BLEND);
			    
			    SimpleText.drawString("TO TRY AGAIN PRESS 'R'", 200, 320);
			    SimpleText.drawString("GOT THAT RAT", 240,330);
			    SimpleText.drawString("PREVIOUS ATTEMPTS : ", 220,310);
			    for (String attempt : scores){
			    	int i =  scores.indexOf(attempt);
			    	SimpleText.drawString("ATTEMPT " + i + " : " + attempt + " SECONDS.", 200,300 - i*10);
			    }
			    
			    glDisable(GL_COLOR_MATERIAL);
			    startTime = System.currentTimeMillis()/1000;
			    Display.update();
			    
			    pollInput();
			    
			    updateFPS();
	        }
        }

        Display.destroy();
    }
    
    public boolean isColliding(int x, int y){
		int dx = rat.getX() - playerX;
		int dy = rat.getY() - playerY;
		double d = Math.sqrt(dx * dx + dy * dy);
		if 	(d <= rat.getRadius() + playerRadius){
			gameComplete = true;
        	scores.add(deltaTime);
			return true;
		}
		return false;
    }
    
    public boolean outOfBounds(int x, int y){
    	if (x > -77 && y>-77 && x<=0 && y<=0 ) return false;
    	else return true;
    	
    }
    
    public void pollInput() {
    	glLoadIdentity();
    	while (Keyboard.next()){
	    	if (Keyboard.isKeyDown(Keyboard.KEY_A)){
    			if (!isColliding(playerX-distX,playerY) && !outOfBounds(playerX-distX,playerY)) {
    				glTranslatef(distX,0,0);
    				playerX -= 3;
    			}
	        }
	    	else if (Keyboard.isKeyDown(Keyboard.KEY_D)) {
    			if (!isColliding(playerX+distX,playerY) && !outOfBounds(playerX+distX,playerY)){
    				glTranslatef(-distX,0,0);
    				playerX += 3;
    			}
	    	}
	    	else if (Keyboard.isKeyDown(Keyboard.KEY_W)) {
				if (!isColliding(playerX,playerY-distZ) && !outOfBounds(playerX,playerY-distZ)){
	    			glTranslatef(0,0,distZ);
					playerY -= 3;
				}
	    	}
	    	else if (Keyboard.isKeyDown(Keyboard.KEY_S)) {
	    		if (!isColliding(playerX,playerY+distZ)  && !outOfBounds(playerX,playerY+distZ)){
	    			glTranslatef(0,0,-distZ);
					playerY += 3;
	    		}	
	    	}
	    	else if (Keyboard.isKeyDown(Keyboard.KEY_R)){
	    		if (gameComplete == true){
	    			gameComplete = false;
	    			glLoadIdentity();
		        	matrixData = BufferUtils.createFloatBuffer(16);
		        	glGetFloat(GL_MODELVIEW_MATRIX, matrixData);
		        	playerMatrix.load(matrixData);
	    			
		        	Random r = new Random();
		    		rat.setX(-r.nextInt(70));
		    		rat.setY(-r.nextInt(70));
		    		
		    		playerX = 0;
		    		playerY = 0;
	    		}
	    	}
	    	else if (Keyboard.isKeyDown(Keyboard.KEY_1)){
	    		glPolygonMode( GL_FRONT_AND_BACK, GL_LINE );
	    	}    	
	    	else if (Keyboard.isKeyDown(Keyboard.KEY_2)){
	    		glPolygonMode( GL_FRONT_AND_BACK, GL_FILL );
	    	}
    	}

    	matrixData = BufferUtils.createFloatBuffer(16);
    	glGetFloat(GL_MODELVIEW_MATRIX, matrixData);
    	increPlayerMatrix.load(matrixData);    	
    }
    
    public void updateFPS() {
        if (getTime() - lastFPS > 1000) {
            Display.setTitle("RatFinder - FPS: " + fps); 
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