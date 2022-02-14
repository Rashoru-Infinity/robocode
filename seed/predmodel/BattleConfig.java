package seed.predmodel;

import java.awt.geom.Point2D;

import robocode.Robot;

public class BattleConfig {
	protected Point2D.Double fieldSize = new Point2D.Double();
	public static final double BODYWIDTH = 36; 
	
	public BattleConfig(Robot robot) {
		double x = robot.getBattleFieldWidth();
		double y = robot.getBattleFieldHeight();
		fieldSize.setLocation(x, y);
	}
	
	public double getFieldWidth() {
		return fieldSize.getX();
	}
	
	public double getFieldHeight() {
		return fieldSize.getY();
	}
}
