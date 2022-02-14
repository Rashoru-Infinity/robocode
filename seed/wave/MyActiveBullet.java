package seed.wave;

import java.awt.geom.Point2D;

import seed.dataset.BulletData;
import robocode.util.Utils;

public class MyActiveBullet extends Wave {
	protected BulletData bullet;
	public MyActiveBullet(BulletData bullet) {
		this.bullet = bullet;
	}
	
	protected void setOffset(double x, double y) {
		double posiRad = this.directAngle + Math.PI / 36;
		double negaRad = this.directAngle - Math.PI / 36;
		Point2D.Double pPos = getLocation(posiRad);
		Point2D.Double nPos = getLocation(negaRad);
		Point2D.Double dirPos = getLocation(this.directAngle);
		double posiDist = Point2D.distance(pPos.getX(), pPos.getY(), x, y);
		double negaDist = Point2D.distance(nPos.getX(), nPos.getY(), x, y);
		int sign = posiDist <= negaDist ? 1 : -1;
		Point2D.Double v1 = new Point2D.Double(x - fireLocation.getX(),
				y - fireLocation.getY());
		Point2D.Double v2 = new Point2D.Double(dirPos.getX() - fireLocation.getX(),
				dirPos.getY() - fireLocation.getY());
		bullet.setRadian(getAngle(v1, v2) * sign);
	}
	
	public BulletData getBullet(double enmX, double enmY) {
		setOffset(enmX, enmY);
		return bullet;
	}
	
	protected Point2D.Double getLocation(double angle) {
		double x = fireLocation.getX() + distanceTraveled * Math.sin(angle);
		double y = fireLocation.getY() + distanceTraveled * Math.cos(angle);
		return new Point2D.Double(x, y);
	}
	
	protected double getAngle(Point2D.Double v1, Point2D.Double v2) {
		double cos = (v1.getX() * v2.getX() + v1.getY() * v2.getY()) /
				(Point2D.distance(0, 0, v1.getX(), v1.getY())
						* Point2D.distance(0, 0, v2.getX(), v2.getY()));
		if (Utils.isNear(Math.abs(cos), Math.abs(1.00000))) {
			cos = Math.pow(10, -10);
			for (double x = 0.9;x > Math.pow(10, -9);x /= 10) {
				cos += x;
			}
		}

		return Math.acos(cos);
	}
}
