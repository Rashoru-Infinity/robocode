package seed.dataset;

import robocode.Rules;
import robocode.ScannedRobotEvent;
import seed.Anastasia;
import seed.predmodel.BattleConfig;

import java.awt.geom.Point2D;

public class BulletData extends DatasetElement {	
	protected double radian;
	protected double distance;
	protected double bulletSpeed;
	
	protected Point2D.Double myVec;
	protected Point2D.Double enmVec;
	
	public void setSample(Anastasia robot, ScannedRobotEvent e, int xDim, int yDim) {
		double absBearing = e.getBearingRadians() + robot.getHeadingRadians();
		this.xDim = xDim;
		this.yDim = yDim;
		
		Point2D.Double myVec = new Point2D.Double(robot.getVelocity() * Math.sin(robot.getHeadingRadians()),
				robot.getVelocity() * Math.cos(robot.getHeadingRadians()));
		myVec = new Point2D.Double(myVec.getX() * Math.sin(-absBearing) - myVec.getY() * -Math.cos(-absBearing),
				myVec.getX() * Math.cos(-absBearing) + myVec.getY() * Math.sin(-absBearing));
		Point2D.Double enmVec = new Point2D.Double(e.getVelocity() * Math.sin(e.getHeadingRadians()),
				e.getVelocity() * Math.cos(e.getHeadingRadians()));
		enmVec = new Point2D.Double(enmVec.getX() * Math.cos(-absBearing) - enmVec.getY() * Math.sin(-absBearing),
				enmVec.getX() * Math.sin(-absBearing) + enmVec.getY() * Math.cos(-absBearing));
		this.radian = 0;
		this.myVec = myVec;
		this.enmVec = enmVec;
		bulletSpeed = Rules.getBulletSpeed(Anastasia.myBulletPower);
		distance = e.getDistance();
		normalize(robot, Anastasia.bc);
	}

	public double getRadian() {
		return radian;
	}
	
	public void setRadian(double radian) {
		this.radian = radian;
	}
	
	public Point2D.Double getMyVec() {
		return myVec;
	}
	
	public void setMyVec(Point2D.Double myVec) {
		this.myVec = myVec;
	}
	
	public Point2D.Double getEnmVec() {
		return enmVec;
	}
	
	public void setEnmVec(Point2D.Double enmVec) {
		this.enmVec = enmVec;
	}

	private void normalize(Anastasia robot, BattleConfig conf) {
		double x = (myVec.getX() + Rules.MAX_VELOCITY) / (Rules.MAX_VELOCITY + Rules.MAX_VELOCITY);
		double y = (myVec.getY() + Rules.MAX_VELOCITY) / (Rules.MAX_VELOCITY + Rules.MAX_VELOCITY);
		myVec.setLocation(x, y);
		x = (enmVec.getX() + Rules.MAX_VELOCITY) / (Rules.MAX_VELOCITY + Rules.MAX_VELOCITY);
		y = (enmVec.getY() + Rules.MAX_VELOCITY) / (Rules.MAX_VELOCITY + Rules.MAX_VELOCITY);
		enmVec.setLocation(x, y);
		double maxDistance = Point2D.distance(BattleConfig.BODYWIDTH, BattleConfig.BODYWIDTH,
				conf.getFieldWidth() - BattleConfig.BODYWIDTH, conf.getFieldHeight() - BattleConfig.BODYWIDTH);
		double minBulletSpeed = Rules.getBulletSpeed(Rules.MAX_BULLET_POWER);
		double minDistance = Point2D.distance(BattleConfig.BODYWIDTH, 0, 2 * BattleConfig.BODYWIDTH, 0);
		double maxBulletSpeed = Rules.getBulletSpeed(Rules.MIN_BULLET_POWER);
		distance = (distance - minDistance) / (maxDistance - minDistance);
		bulletSpeed = (bulletSpeed - minBulletSpeed) / (maxBulletSpeed - minBulletSpeed); 
	}
	
	@Override
	public SampleVector getVector() {
		SampleVector vector = new SampleVector(xDim, yDim);
		vector.y[0] = radian;
		vector.x[0] = distance;
		vector.x[1] = bulletSpeed;
		vector.x[2] = myVec.getX();
		vector.x[3] = myVec.getY();
		vector.x[4] = enmVec.getX();
		vector.x[5] = enmVec.getY();
		return vector;
	}
}
