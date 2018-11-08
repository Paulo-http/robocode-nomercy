package leite.paulohf;
import robocode.*;
import java.awt.Color;
import java.awt.geom.*;
import java.util.*;
import robocode.util.Utils;
import static robocode.util.Utils.normalRelativeAngleDegrees;

// API help : http://robocode.sourceforge.net/docs/robocode/robocode/Robot.html

/**
* NoMercy - a robot by Paulo Henrique Leite
*/
public class NoMercy extends AdvancedRobot {

  public boolean forward;
  public int distance = 50;
  public double walk;
  List<ScannedRobotEvent> sre = new ArrayList<ScannedRobotEvent>();

  /**
  * run: NoMercy's default behavior
  */
  public void run() {
    initialAdjust();
    defaultWalk();
    while(true) {
      checkNearWall();
      if (getRadarTurnRemaining() == 0.0)
        turnRadarRightRadians(Double.POSITIVE_INFINITY);
      execute();
    }
  }

  //  is used to setting initial adjust
  public void initialAdjust() {
    setColors(Color.BLACK, Color.BLACK, Color.BLACK, Color.BLACK, Color.BLACK);
    setAdjustRadarForRobotTurn(true);
    setAdjustGunForRobotTurn(true);
    setAdjustRadarForGunTurn(true);
  }

  //  is used to setting default value to walk
  public void defaultWalk() {
    double width = getBattleFieldWidth();
    double height = getBattleFieldHeight();
    this.walk = (width > height) ? width : height;
  }

  //  is used to check if has wall near axis
  public void checkNearWall() {
    boolean wallNearAxisX = (getX() <= this.distance || getX() > getBattleFieldWidth() - this.distance);
    boolean wallNearAxisY = (getY() <= this.distance || getY() > getBattleFieldHeight() - this.distance);
    if(wallNearAxisX || wallNearAxisY)
      setBack(this.walk);
    else
      setAhead(this.walk);
  }

  //  is used to revert forward flag
  public void revertForward() {
    if (this.forward)
      setBack(this.walk);
    else
      setAhead(this.walk);
    this.forward = !this.forward;
  }

  //  is used to track enemy by my position and angle that my opponent is in relation me
  public void logicRadarMovement(ScannedRobotEvent e) {
    double abs = getHeadingRadians() + e.getBearingRadians();
    double displacement = e.getVelocity() * Math.sin(e.getHeadingRadians() - abs) / Rules.getBulletSpeed(e.getEnergy());
    double angle = (Utils.normalRelativeAngle(abs - getGunHeadingRadians() + displacement));

    setTurnRadarLeftRadians(getRadarTurnRemainingRadians());
    angle = (Utils.normalRelativeAngle(abs - getGunHeadingRadians() + displacement));
    setTurnGunRightRadians(angle);
    setTurnRight(normalRelativeAngleDegrees(e.getBearing() + 100));

    if (this.forward)
      setAhead(400);

    setFire(3);
    angle = 5 * Utils.normalRelativeAngle(getHeadingRadians() + e.getBearingRadians() - getRadarHeadingRadians());
    setTurnRadarRightRadians(angle);
    scan();
  }

  //  is used to track enemy and do a perfect shot
  public void iaLogicRadarMovement(ScannedRobotEvent e) {
    sre.add(e);
    double enemy = getHeadingRadians() + e.getBearingRadians();
    double angle = Utils.normalRelativeAngle(enemy - getRadarHeadingRadians());
    double turn = Math.min(Math.atan(36.0 / e.getDistance()), Rules.RADAR_TURN_RATE_RADIANS);

    if (angle < 0)
    	angle -= turn;
    else
    	angle += turn;

    setTurnRadarRightRadians(angle);

    double myX = getX();
    double myY = getY();
    double abs = getHeadingRadians() + e.getBearingRadians();
    double enemyX = getX() + e.getDistance() * Math.sin(abs);
    double enemyY = getY() + e.getDistance() * Math.cos(abs);

    double enemyHeading = e.getHeadingRadians();
    double oldEnemyHeading = sre.get(sre.size()-2).getHeadingRadians();
    double enemyHeadingChange = enemyHeading - oldEnemyHeading;
    oldEnemyHeading = enemyHeading;

    double attempts = 0;
    double battleFieldHeight = getBattleFieldHeight();
    double battleFieldWidth = getBattleFieldWidth();
    double enemyVelocity = e.getVelocity();
    double bulletPower = Math.min(3.0,getEnergy());

    while((++attempts) * (20.0 - 3.0 * bulletPower) < Point2D.Double.distance(myX, myY, enemyX, enemyY)) {
      enemyX += Math.sin(enemyHeading) * enemyVelocity;
      enemyY += Math.cos(enemyHeading) * enemyVelocity;
      enemyHeading += enemyHeadingChange;

      boolean trackX = (enemyX < 18.0 || enemyX > battleFieldWidth - 18.0);
      boolean trackY = (enemyY < 18.0 || enemyY > battleFieldHeight - 18.0);

      if(trackX || trackY) {
        enemyX = Math.min(Math.max(18.0, enemyX), battleFieldWidth - 18.0);
        enemyY = Math.min(Math.max(18.0, enemyY), battleFieldHeight - 18.0);
        break;
      }
    }

    double theta = Utils.normalAbsoluteAngle(Math.atan2(enemyX - getX(), enemyY - getY()));
    setTurnRadarRightRadians(Utils.normalRelativeAngle(abs - getRadarHeadingRadians()));
    setTurnGunRightRadians(Utils.normalRelativeAngle(theta - getGunHeadingRadians()));

    bulletPower = (getEnergy() > 5) ? bulletPower : 1;
    fire(bulletPower);
  }

  public void onHitWall(HitWallEvent e) {
    revertForward();
  }

  public void onScannedRobot(ScannedRobotEvent e) {
    // logicRadarMovement(e);
    iaLogicRadarMovement(e);
  }

  public void onRobotWall(HitRobotEvent e) {
    setTurnGunRightRadians(e.getBearingRadians());
    revertForward();
  }

  public void onHitByBullet(HitByBulletEvent e) {
    revertForward();
  }

  public void onHitRobot(HitRobotEvent e) {
		if (e.isMyFault())
			revertForward();
	}

}
