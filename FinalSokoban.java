import tester.*;
import javalib.impworld.*;
import java.awt.Color;
import javalib.worldimages.*;
import java.util.ArrayList;

// represents the entire block makeup of the game
class Level {
  ArrayList<ArrayList<ACell>> ground;
  ArrayList<ArrayList<ACell>> content;

  Level(String groundDesc, String contentDesc) {
    this.ground = this.configureLevel(groundDesc);
    this.content = this.configureLevel(contentDesc);
  }

  // checks if the Sokoban level is won
  boolean levelWon() {
    for (int r = 0; r < this.ground.size(); r++) {
      for (int c = 0; c < this.ground.get(r).size(); c++) {
        if (!this.ground.get(r).get(c).checkTarget(this.content.get(r).get(c))) {
          return false;
        }
      }
    }
    return true;
  }

  // returns the list of lists
  ArrayList<ArrayList<ACell>> configureLevel(String desc) {
    ArrayList<ArrayList<ACell>> arr = new ArrayList<ArrayList<ACell>>();
    ArrayList<ACell> cells = new ArrayList<ACell>();
    for (int i = 0; i < desc.length(); i++) {
      String c = desc.substring(i, i + 1);
      if (c.equals("\n")) {
        arr.add(cells);
        cells = new ArrayList<ACell>();
      }
      else {
        cells.add(this.configureCell(c));
      }
    }
    arr.add(cells);
    return arr;
  }

  // returns whether the player is on the board
  boolean hasPlayer() {
    return this.findPlayerRow() != -1;
  }

  // returns the image of the game
  WorldImage finalImage() {
    WorldImage rr = new EmptyImage();
    for (int r = 0; r < this.ground.size(); r++) {
      WorldImage cc = new EmptyImage();
      for (int c = 0; c < this.ground.get(r).size(); c++) {
        cc = new BesideImage(cc, new OverlayImage(this.ground.get(r).get(c).imageAdder(),
            this.content.get(r).get(c).imageAdder()));
      }
      rr = new AboveImage(rr, cc);
    }
    return rr;
  }

  // outputs the corresponding Cell given the String
  ACell configureCell(String s) {
    if (s.equals("W")) {
      return new Wall();
    }
    if (s.equals("C")) {
      return new Crate();
    }
    if (s.equals("Y") || s.equals("G") || s.equals("B") || s.equals("R")) {
      return new Target(s);
    }
    if (s.equals("y") || s.equals("g") || s.equals("b") || s.equals("r")) {
      return new Trophy(s);
    }
    if (s.equals(">") || s.equals("<") || s.equals("^") || s.equals("v")) {
      return new Player(s);
    }
    if (s.equals("_")) {
      return new Blank();
    }
    if (s.equals("H")) {
      return new Hole();
    }
    if (s.equals("I")) {
      return new Ice();
    }

    throw new IllegalArgumentException("Invalid Character!");
  }

  // finds the row that the player is located
  int findPlayerRow() {
    for (int i = 0; i < this.content.size(); i++) {
      if (this.findPlayerColumn(i) != -1) {
        return i;
      }
    }
    return -1;
  }

  // finds the column that the player is located in the supplied row
  int findPlayerColumn(int r) {
    if (r == -1) {
      return -1;
    }
    for (int i = 0; i < this.content.get(r).size(); i++) {
      if (this.content.get(r).get(i).indicator.equals("^")
          || this.content.get(r).get(i).indicator.equals("<")
          || this.content.get(r).get(i).indicator.equals(">")
          || this.content.get(r).get(i).indicator.equals("v")) {
        return i;
      }
    }
    return -1;
  }

  // handles player movement depending on key input
  void movePlayer(String key) {
    int row = this.findPlayerRow();
    int col = this.findPlayerColumn(this.findPlayerRow());
    if (key.equals(">")) {
      this.content.get(row).get(col).moveRight(row, col, this);
    }
    if (key.equals("^")) {
      this.content.get(row).get(col).moveUp(row, col, this);
    }
    if (key.equals("<")) {
      this.content.get(row).get(col).moveLeft(row, col, this);
    }
    if (key.equals("v")) {
      this.content.get(row).get(col).moveDown(row, col, this);
    }
  }
}

// visualizes the game
class SokobanLevel extends World {
  Level game;
  ArrayList<Level> history;
  int count;

  SokobanLevel(Level game) {
    this.game = game;
  }

  // makes the image of the game in the screen
  public WorldScene makeScene() {
    WorldScene screen = getEmptyScene();
    screen.placeImageXY(new AboveImage(new TextImage("Number of moves: ", this.count, Color.BLACK),
        this.game.finalImage()), 400, 400);
    screen.placeImageXY(this.game.finalImage(), 400, 400);
    return screen;
  }

  // displays the effect of keyboard inputs
  public void onKeyEvent(String key) {
    if (key.equals("up") || key.equals("down") || key.equals("left") || key.equals("right")) {
      this.game.movePlayer(key);
      this.history.add(this.game);
      count++;
    }
    if (key.equals("u")) {
      this.game = this.history.remove(this.history.size() - 1);
      count++;
    }
  }

  // detects when the game ends
  // either you win
  // or your player is no longer detected since it fell into a hole hence losing
  public void onTick() {
    if (this.game.levelWon()) {
      this.endOfWorld("You win");
    }
    if (this.game.hasPlayer()) {
      this.endOfWorld("You lose");
    }
  }
}

// represents a block of the game
interface ICell {

  // adds the image of each individual cells
  WorldImage imageAdder();

  // EQUALITY: We are using exclusive equality when comparing Targets, Trophies
  // because there is a String that differs, we are checking more than the object.
  // returns true until it compares a trophy with a target
  boolean checkTarget(ACell t);

  void moveUp(int r, int c, Level level);
  
  void moveDown(int r, int c, Level level);
  
  void moveLeft(int r, int c, Level level);
  
  void moveRight(int r, int c, Level level);
}

// represents ICells
abstract class ACell implements ICell {
  String indicator;

  ACell(String indicator) {
    this.indicator = indicator;
  }
  
  public boolean checkTarget(ACell cell) {
    if ((this.indicator.equals("Y") || this.indicator.equals("B") || this.indicator.equals("G")
        || this.indicator.equals("R"))) {
      return this.indicator.equals(cell.indicator.toUpperCase());
    }
    return true;
  }
  
  public void moveUp(int r, int c, Level level) {
    if (r > 0) {
      level.content.get(r - 1).get(c).moveUp(r - 1, c, level);
      if (level.ground.get(r - 1).get(c).indicator.equals("I")) {
        level.content.get(r).get(c).moveUp(r, c, level);
      }
    }
  }

  public void moveDown(int r, int c, Level level) {
    if (level.content.size() > r + 1) {
      level.content.get(r + 1).get(c).moveDown(r + 1, c, level);
      if (level.ground.get(r + 1).get(c).indicator.equals("I")) {
        level.content.get(r).get(c).moveDown(r, c, level);
      }
    }
  }

  public void moveLeft(int r, int c, Level level) {
    if (c > 0) {
      level.content.get(r).get(c - 1).moveLeft(r, c - 1, level);
      if (level.ground.get(r).get(c - 1).indicator.equals("I")) {
        level.content.get(r).get(c).moveLeft(r, c, level);
      }
    }
  }

  public void moveRight(int r, int c, Level level) {
    if (level.content.get(r).size() > c + 1) {
      level.content.get(r).get(c + 1).moveRight(r, c + 1, level);
      if (level.ground.get(r).get(c + 1).indicator.equals("I")) {
        level.content.get(r).get(c).moveRight(r, c, level);
      }
    }
  }
}

// represents a wall
class Wall extends ACell {

  Wall() {
    super("W");
  }

  public WorldImage imageAdder() { // adds image for walls
    return new FromFileImage("src/Images/wall.png");
  }
  
  public void moveUp(int r, int c, Level level) {
    int pr = level.findPlayerRow();
    int pc = level.findPlayerColumn(level.findPlayerRow());
    level.content.get(pr).remove(pc);
    level.content.get(pr).add(pc, new Player("^"));
  }

  public void moveDown(int r, int c, Level level) {
    int pr = level.findPlayerRow();
    int pc = level.findPlayerColumn(level.findPlayerRow());
    level.content.get(pr).remove(pc);
    level.content.get(pr).add(pc, new Player("v"));
  }

  public void moveLeft(int r, int c, Level level) {
    int pr = level.findPlayerRow();
    int pc = level.findPlayerColumn(level.findPlayerRow());
    level.content.get(pr).remove(pc);
    level.content.get(pr).add(pc, new Player(">"));
  }

  public void moveRight(int r, int c, Level level) {
    int pr = level.findPlayerRow();
    int pc = level.findPlayerColumn(level.findPlayerRow());
    level.content.get(pr).remove(pc);
    level.content.get(pr).add(pc, new Player("<"));
  }
}

// represents a trophy
class Trophy extends ACell {

  Trophy(String indicator) {
    super(indicator);
  }

  public WorldImage imageAdder() { // adds image for trophies
    if (this.indicator.equals("y")) {
      return new FromFileImage("src/Images/yellowTroph.png");
    }
    else if (this.indicator.equals("g")) {
      return new FromFileImage("src/Images/greenTroph.png");
    }
    else if (this.indicator.equals("r")) {
      return new FromFileImage("src/Images/redTroph.png");
    }
    else {
      return new FromFileImage("src/Images/blueTroph.png");
    }
  }
}

// represents a target
class Target extends ACell {

  Target(String indicator) {
    super(indicator);
  }

  public WorldImage imageAdder() { // adds target images
    if (this.indicator.equals("Y")) {
      return new FromFileImage("src/Images/yellowTarg.jpg");
    }
    else if (this.indicator.equals("G")) {
      return new FromFileImage("src/Images/greenTarg.jpg");
    }
    else if (this.indicator.equals("R")) {
      return new FromFileImage("src/Images/redTarg.jpg");
    }
    else if (this.indicator.equals("B")) {
      return new FromFileImage("src/Images/blueTarg.png");
    }
    throw new IllegalArgumentException("Target does not exist!");
  }
}

// represents a crate
class Crate extends ACell {

  Crate() {
    super("C");
  }

  public WorldImage imageAdder() { // adds crate image
    return new FromFileImage("src/Images/crate.png");
  }
}

// represents the player
class Player extends ACell {

  Player(String p) {
    super(p);
  }

  public WorldImage imageAdder() { // adds person image
    return new FromFileImage("src/Images/person.png");
  }
}

// represents a blank space
class Blank extends ACell {

  Blank() {
    super("_");
  }

  public WorldImage imageAdder() { // adds blank image
    return new RectangleImage(40, 40, OutlineMode.OUTLINE, Color.black);
  }
  
  public void moveUp(int r, int c, Level level) {
    ACell object = level.content.get(r + 1).get(c);
    level.content.get(r).remove(c);
    level.content.get(r).add(c, object);
    level.content.get(r + 1).remove(c);
    level.content.get(r + 1).add(c, new Blank());
  }

  public void moveDown(int r, int c, Level level) {
    ACell object = level.content.get(r - 1).get(c);
    level.content.get(r).remove(c);
    level.content.get(r).add(c, object);
    level.content.get(r - 1).remove(c);
    level.content.get(r - 1).add(c, new Blank());
  }

  public void moveLeft(int r, int c, Level level) {
    level.content.get(r).remove(c);
    level.content.get(r).add(c + 1, new Blank());
  }

  public void moveRight(int r, int c, Level level) {
    level.content.get(r).remove(c);
    level.content.get(r).add(c, new Blank());
  }
}

// represents a hole
class Hole extends ACell {

  Hole() {
    super("H");
  }

  public WorldImage imageAdder() { // adds hole image
    return new FromFileImage("src/Images/hole.png");
  }
  
  public void moveUp(int r, int c, Level level) {
    level.content.get(r).remove(c);
    level.content.get(r).add(c, new Blank());
    level.content.get(r + 1).remove(c);
    level.content.get(r + 1).add(c, new Blank());
  }

  public void moveDown(int r, int c, Level level) {
    level.content.get(r).remove(c);
    level.content.get(r).add(c, new Blank());
    level.content.get(r - 1).remove(c);
    level.content.get(r - 1).add(c, new Blank());
  }

  public void moveLeft(int r, int c, Level level) {
    level.content.get(r).remove(c);
    level.content.get(r).remove(c);
    level.content.get(r).add(c, new Blank());
    level.content.get(r).add(c, new Blank());
  }

  public void moveRight(int r, int c, Level level) {
    level.content.get(r).remove(c);
    level.content.get(r).add(c, new Blank());
    level.content.get(r).remove(c - 1);
    level.content.get(r).add(c - 1, new Blank());
  }
}

// represents ice
class Ice extends ACell {
  
  Ice() {
    super("I");
  }
  
  // adds ice image
  public WorldImage imageAdder() {
    return new FromFileImage("src/Images/ice.png");
  }
}

class Examples {

  // more advanced Sokoban level
  Level ex1 = new Level(
      "________\n" + "___R____\n" + "________\n" + "_B____Y_\n" + "________\n" + "___G____\n"
          + "________",
      "__WWW___\n" + "__W_WW__\n" + "WWWr_WWW\n" + "W_b>yB_W\n" + "WW_gWWWW\n" + "_WW_W___\n"
          + "__WWW___");

  // more advanced Sokoban Level
  Level ex2Won = new Level(
      "________\n" + "___R____\n" + "________\n" + "_B____Y_\n" + "________\n" + "___G____\n"
          + "________",
      "__WWW___\n" + "__WrWW__\n" + "WWW__WWW\n" + "Wb_>_ByW\n" + "WW__WWWW\n" + "_WWgW___\n"
          + "__WWW___");

  // extra sample level used for testing
  Level ex3 = new Level("__\n" + "B_\n" + "__\n", "__\n" + "__\n" + "_^\n");

  // empty sokoban level
  Level blank = new Level("_W_W_W_W\n" + "________\n" + "________",
      "________\n" + "________\n" + "________");

  ArrayList<ACell> list = new ArrayList<ACell>();
  ArrayList<ArrayList<ACell>> lists = new ArrayList<ArrayList<ACell>>();

  // creates simple level of Sokoban
  Level simpleLevel = new Level("___\n" + "__", "__\n" + "__");

  // tests the findPlayerRow findPlayerCollumn and hasPlayer methods
  boolean testFindPlayerRowCollumnhasPlayer(Tester t) {
    return t.checkExpect(ex1.findPlayerRow(), 3)
        && t.checkExpect(ex1.findPlayerColumn(ex1.findPlayerRow()), 3)
        && t.checkExpect((blank.findPlayerRow()), -1)
        && t.checkExpect(blank.findPlayerColumn(blank.findPlayerRow()), -1)
        && t.checkExpect(ex1.hasPlayer(), true) // player is on board
        && t.checkExpect(blank.hasPlayer(), false); // player is not on board
  }

  // tests configure cell method giving right object
  boolean testConfigureCell(Tester t) {
    return t.checkExpect(simpleLevel.configureCell("_"), new Blank())
        && t.checkExpect(simpleLevel.configureCell("R"), new Target("R"))
        && t.checkExpect(simpleLevel.configureCell("r"), new Trophy("r"))
        && t.checkExpect(simpleLevel.configureCell("^"), new Player("^"))
        && t.checkExpect(simpleLevel.configureCell("W"), new Wall());
  }

  // DOES NOT WORK
  boolean testNotWonAndWon(Tester t) {
    // checks a level that is list
    return t.checkExpect(ex1.levelWon(), false) && t.checkExpect(ex3.levelWon(), false)
        && t.checkExpect(ex2Won.levelWon(), true); // checks a level that is won.
  }

  // tests the configureLevel method and the fields are correct
  boolean testConfigureLevel(Tester t) {
    ArrayList<ACell> listG = new ArrayList<ACell>();
    ArrayList<ACell> listC = new ArrayList<ACell>();
    listG.add(new Blank());
    listG.add(new Blank());
    listC.add(new Blank());
    listC.add(new Wall());
    ArrayList<ArrayList<ACell>> list = new ArrayList<ArrayList<ACell>>();
    list.add(listC);
    list.add(listC);

    return t.checkExpect(simpleLevel.ground.get(1), listG) // returns second column of ground array
        && t.checkExpect(simpleLevel.ground.get(0).get(0), new Blank());
  }

  // tests the checkTarget method
  boolean testcheckTarget(Tester t) {
    ACell targ1 = new Target("B");
    ACell targ2 = new Target("R");
    ACell troph = new Trophy("b");
    ACell w = new Wall();
    return t.checkExpect(troph.checkTarget(targ1), true) // trophy on target returns true
        && t.checkExpect(targ1.checkTarget(troph), true) // checks matching colors
        && t.checkExpect(targ2.checkTarget(troph), false) // checks unmatching colors
        && t.checkExpect(troph.checkTarget(w), true);
  }

  Level first = new Level(
      "________\n" + "________\n" + "________\n" + "________\n" + "________\n" + "________\n"
          + "________\n" + "________\n" + "________",
      "__WWWWW_\n" + "WWW___W_\n" + "W_>C__W_\n" + "WWW_C_W_\n" + "W_WWC_W_\n" + "W_W___WW\n"
          + "WC_CCC_W\n" + "W______W\n" + "WWWWWWWW");
  int r = this.first.findPlayerRow();
  int c = this.first.findPlayerColumn(r);
  
  boolean testMove(Tester t) {
    this.first.movePlayer("up");
    return t.checkExpect(this.first.content.get(r).get(c), new Player("^"));
  }
  
  boolean testMove1(Tester t) {
    this.first.movePlayer("right");
    return t.checkExpect(this.first.content.get(r).get(c - 1), new Blank())
        && t.checkExpect(this.first.content.get(r).get(c + 1), new Crate());
  }
  
  boolean testMove2(Tester t) {
    this.first.movePlayer("right");
    this.first.movePlayer("up");
    this.first.movePlayer("right");
    this.first.movePlayer("down");
    return t.checkExpect(this.first.content.get(r).get(c - 1), new Blank())
        && t.checkExpect(this.first.content.get(r).get(c + 1), new Wall())
        && t.checkExpect(this.first.content.get(r + 1).get(c), new Crate());
  }
  
  boolean testMove3(Tester t) {
    this.first.movePlayer("down");
    this.first.movePlayer("down");
    return t.checkExpect(this.first.content.get(r).get(c - 1), new Crate())
        && t.checkExpect(this.first.content.get(r).get(c + 1), new Wall())
        && t.checkExpect(this.first.content.get(r + 1).get(c), new Crate());
  }
  
  Level second = new Level(
      "________\n" + "________\n" + "_____I__\n" + "________",
      "__WWWWW_\n" + "WWW___W_\n" + "W_>C__W_\n" + "WWWWWWWW");
  
  boolean testSlide(Tester t) {
    this.first.movePlayer("right");
    return t.checkExpect(this.first.content.get(r).get(c - 1), new Blank())
        && t.checkExpect(this.first.content.get(r).get(c + 1), new Blank())
        && t.checkExpect(this.first.content.get(r).get(c + 2), new Crate());
  }
 
  // runs the game (issues with images)
  //  void testGame(Tester t) {
  //    SokobanLevel level = new SokobanLevel(simpleLevel);
  //    level.bigBang(800, 800);
  //  }
}