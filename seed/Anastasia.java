package seed;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;

import robocode.AdvancedRobot;
import robocode.HitByBulletEvent;
import robocode.Rules;
import robocode.ScannedRobotEvent;
import robocode.util.Utils;
import seed.dataset.BulletData;
import seed.predmodel.BattleConfig;
import seed.predmodel.Regressor;
import seed.wave.MyActiveBullet;
import seed.wave.Wave;

public class Anastasia extends AdvancedRobot {
	public static int BINS = 69;
	public static double surfStats[] = new double[BINS];
	public Point2D.Double myLocation;
	public Point2D.Double enemyLocation;

	public ArrayList<Wave> Waves;
	public ArrayList<Integer> surfDirections;
	public ArrayList<java.lang.Double> surfAbsBearings;

	public static double _oppEnergy = 100.0;
	
	public static Rectangle2D.Double fieldRect = null;
	public static double WALL_STICK = 160;
	
	public static Regressor model = new Regressor(100, 100, 6, 4);
	
	public static BattleConfig bc;
	
	public static ArrayList<MyActiveBullet> activeBullet = new ArrayList<>();
	public static double myBulletPower = 2.0;
	public static double enmBulletPower = 2.0;
	
	public void run() {
		bc = new BattleConfig(this);
		if (fieldRect == null) {
			fieldRect = new Rectangle2D.Double
			(18, 18, getBattleFieldWidth()- 36, getBattleFieldHeight() - 36);
		}
		Waves = new ArrayList<Wave>();
		surfDirections = new ArrayList<>();
		surfAbsBearings = new ArrayList<>();
		
		setBodyColor(Color.white);
		setGunColor(Color.white);
		setRadarColor(Color.white);
		setScanColor(Color.white);
		setAdjustGunForRobotTurn(true);
		setAdjustRadarForGunTurn(true);
		
		do {
			turnRadarRightRadians(java.lang.Double.POSITIVE_INFINITY);
		} while (true);
	}

	public void onScannedRobot(ScannedRobotEvent e) {
		myBulletPower = enmBulletPower * getEnergy() / e.getEnergy();
		myLocation = new Point2D.Double(getX(), getY());
		if (e.getDistance() < 250) {
			myBulletPower *= 250 / e.getDistance();
		}
		if (myBulletPower > Rules.MAX_BULLET_POWER) {
			myBulletPower = Rules.MAX_BULLET_POWER;
		}
		while (Rules.getBulletDamage(myBulletPower) > e.getEnergy()) {
			if (myBulletPower - Rules.MIN_BULLET_POWER < Rules.MIN_BULLET_POWER) {
				break;
			}
			myBulletPower -= 0.1;
		}
		double lateralVelocity = getVelocity()*Math.sin(e.getBearingRadians());
		double absBearing = e.getBearingRadians() + getHeadingRadians();
		
		setTurnRadarRightRadians(Utils.normalRelativeAngle(absBearing - getRadarHeadingRadians()) * 2);
		BulletData shotDataset = new BulletData();
		shotDataset.setSample(this, e, 8, 1);
		setTurnGunRightRadians(Utils.normalRelativeAngle(absBearing + model.predict(shotDataset) - getGunHeadingRadians()));
		
		surfDirections.add(0, lateralVelocity >= 0 ? 1 : -1);
		surfAbsBearings.add(0, absBearing + Math.PI);
		
		
		double bulletPower = _oppEnergy - e.getEnergy();
		if (bulletPower < 3.01 && bulletPower > 0.09
			&& surfDirections.size() > 2) {
			enmBulletPower = bulletPower;
			Wave ew = new Wave();
			ew.fireTime = getTime() - 1;
			ew.bulletSpeed = Rules.getBulletSpeed(bulletPower);
			ew.distanceTraveled = ew.bulletSpeed;
			ew.direction = ((Integer)surfDirections.get(2)).intValue();
			ew.directAngle = surfAbsBearings.get(2).doubleValue();
			ew.fireLocation = (Point2D.Double)enemyLocation.clone();
			
			Waves.add(ew);
		}
		
		_oppEnergy = e.getEnergy();
		
		enemyLocation = project(myLocation, absBearing, e.getDistance());
		
		if (Utils.isNear(getGunHeat(), 0.0) && getEnergy() - myBulletPower > 0.1) {
			MyActiveBullet mab = new MyActiveBullet(shotDataset);
			mab.fireTime = getTime();
			mab.bulletSpeed = Rules.getBulletSpeed(2);
			mab.distanceTraveled = 0;
			mab.directAngle = absBearing;
			mab.fireLocation = new Point2D.Double(getX(), getY());
			activeBullet.add(mab);
			setFire(myBulletPower);
		}
		updateWaves(enemyLocation);
		doSurfing();
	}

	public void updateWaves(Point2D.Double enemyLocation) {
		for (int x = 0; x < Waves.size(); x++) {
			Wave ew = (Wave)Waves.get(x);
			
			ew.distanceTraveled = (getTime() - ew.fireTime) * ew.bulletSpeed;
			if (ew.distanceTraveled >
				myLocation.distance(ew.fireLocation) + 50) {
				Waves.remove(x);
				x--;
			}
		}
		
		if (enemyLocation == null) {
			return ;
		}
		for (int x = 0; x < activeBullet.size(); x++) {
			MyActiveBullet abw = activeBullet.get(x);
			
			abw.distanceTraveled = (getTime() - abw.fireTime) * abw.bulletSpeed;
			if (abw.distanceTraveled >
				enemyLocation.distance(abw.fireLocation) + 50) {
				model.addSample(activeBullet.get(x).getBullet(enemyLocation.getX(), enemyLocation.getY()));
				activeBullet.remove(x);
				x--;
				model.printWeight();
				model.printMSE();
			}
		}
		model.updateModel();
		
	}

	public Wave getClosestSurfableWave() {
	    double closestDistance = java.lang.Double.POSITIVE_INFINITY;
	    Wave surfWave = null;
	
	    for (int x = 0; x < Waves.size(); x++) {
	        Wave ew = (Wave)Waves.get(x);
	        double distance = myLocation.distance(ew.fireLocation)
	            - ew.distanceTraveled;
	
	        if (distance > ew.bulletSpeed && distance < closestDistance) {
	            surfWave = ew;
	            closestDistance = distance;
	        }
	    }
	
	    return surfWave;
	}

	public static int getFactorIndex(Wave ew, Point2D.Double targetLocation) {
	    double offsetAngle = (absoluteBearing(ew.fireLocation, targetLocation)
	        - ew.directAngle);
	    double factor = Utils.normalRelativeAngle(offsetAngle)
	        / maxEscapeAngle(ew.bulletSpeed) * ew.direction;
	
	    return (int)limit(0,
	        (factor * ((BINS - 1) / 2)) + ((BINS - 1) / 2),
	        BINS - 1);
	}

	public void logHit(Wave ew, Point2D.Double targetLocation) {
	    int index = getFactorIndex(ew, targetLocation);
	
	    for (int x = 0; x < BINS; x++) {
	        surfStats[x] += 1.0 / (Math.pow(index - x, 2) + 1);
	    }
	}

	public void onHitByBullet(HitByBulletEvent e) {
		if (!Waves.isEmpty()) {
			Point2D.Double hitBulletLocation = new Point2D.Double(
				e.getBullet().getX(), e.getBullet().getY());
			Wave hitWave = null;
			
			for (int x = 0; x < Waves.size(); x++) {
				Wave ew = (Wave)Waves.get(x);
			
			    if (Math.abs(ew.distanceTraveled -
			        myLocation.distance(ew.fireLocation)) < 50
			        && Utils.isNear(Rules.getBulletSpeed(e.getBullet().getPower()), ew.bulletSpeed)) {
			        hitWave = ew;
			        break;
			    }
			}
			
			if (hitWave != null) {
			    logHit(hitWave, hitBulletLocation);
			
			    Waves.remove(Waves.lastIndexOf(hitWave));
			}
	    }
	}

	public Point2D.Double predictPosition(Wave surfWave, int direction) {
		Point2D.Double predictedPosition = (Point2D.Double)myLocation.clone();
		double predictedVelocity = getVelocity();
		double predictedHeading = getHeadingRadians();
		double maxTurning, moveAngle, moveDir;
	
	    int counter = 0;
	    boolean intercepted = false;
	
		do {
			moveAngle =
				wallSmoothing(predictedPosition, absoluteBearing(surfWave.fireLocation,
	            predictedPosition) + (direction * (Math.PI/2)), direction)
	            - predictedHeading;
			moveDir = 1;
	
			if(Math.cos(moveAngle) < 0) {
				moveAngle += Math.PI;
				moveDir = -1;
			}
	
			moveAngle = Utils.normalRelativeAngle(moveAngle);
	
			maxTurning = Math.PI/720d*(40d - 3d*Math.abs(predictedVelocity));
			predictedHeading = Utils.normalRelativeAngle(predictedHeading
				+ limit(-maxTurning, moveAngle, maxTurning));
	
			predictedVelocity += (predictedVelocity * moveDir < 0 ? 2*moveDir : moveDir);
			predictedVelocity = limit(-Rules.MAX_VELOCITY, predictedVelocity, Rules.MAX_VELOCITY);
	
			predictedPosition = project(predictedPosition, predictedHeading, predictedVelocity);
	
	        counter++;
	
	        if (predictedPosition.distance(surfWave.fireLocation) <
				surfWave.distanceTraveled + (counter * surfWave.bulletSpeed)
				+ surfWave.bulletSpeed) {
				intercepted = true;
	        }
		} while(!intercepted && counter < 500);
	
		return predictedPosition;
	}

	public double checkDanger(Wave surfWave, int direction) {
		int index = getFactorIndex(surfWave,
			predictPosition(surfWave, direction));
		
		return surfStats[index];
	}

	public void doSurfing() {
		Wave surfWave = getClosestSurfableWave();
		
		if (surfWave == null) { return; }
		
		double dangerLeft = checkDanger(surfWave, -1);
		double dangerRight = checkDanger(surfWave, 1);
		
		double goAngle = absoluteBearing(surfWave.fireLocation, myLocation);
		if (dangerLeft < dangerRight) {
		    goAngle = wallSmoothing(myLocation, goAngle - (Math.PI/2), -1);
		} else {
		    goAngle = wallSmoothing(myLocation, goAngle + (Math.PI/2), 1);
		}
		
		setBackAsFront(this, goAngle);
	}

	public double wallSmoothing(Point2D.Double botLocation, double angle, int orientation) {
	    while (!fieldRect.contains(project(botLocation, angle, 160))) {
	        angle += orientation*0.05;
	    }
	    return angle;
	}

	public static Point2D.Double project(Point2D.Double sourceLocation, double angle, double length) {
	    return new Point2D.Double(sourceLocation.x + Math.sin(angle) * length,
	    	sourceLocation.y + Math.cos(angle) * length);
	}

	public static double absoluteBearing(Point2D.Double source, Point2D.Double target) {
	    return Math.atan2(target.x - source.x, target.y - source.y);
	}

	public static double limit(double min, double value, double max) {
		return Math.max(min, Math.min(value, max));
	}

	public static double maxEscapeAngle(double velocity) {
		return Math.asin(Rules.MAX_VELOCITY/velocity);
	}

	public static void setBackAsFront(AdvancedRobot robot, double goAngle) {
		double angle =
			Utils.normalRelativeAngle(goAngle - robot.getHeadingRadians());
		if (Math.abs(angle) > (Math.PI/2)) {
		    if (angle < 0) {
		        robot.setTurnRightRadians(Math.PI + angle);
		    } else {
		        robot.setTurnLeftRadians(Math.PI - angle);
		    }
		    robot.setBack(100);
		} else {
			if (angle < 0) {
				robot.setTurnLeftRadians(-1*angle);
			} else {
				robot.setTurnRightRadians(angle);
			}
			robot.setAhead(100);
		}
	}

	public void onPaint(Graphics2D g) {
		for(int i = 0; i < Waves.size(); i++){
			 g.setColor(java.awt.Color.red);
			 Wave w = (Wave)(Waves.get(i));
			 Point2D.Double center = w.fireLocation;
			
			int radius = (int)w.distanceTraveled;
			
			if(radius - 40 < center.distance(myLocation))
				g.drawOval((int)(center.x - radius ), (int)(center.y - radius), radius*2, radius*2);
		 }
	}
}
