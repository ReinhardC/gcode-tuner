package main.com.specularity.printing.GCodes;

public class Point2D {
    public double x, y;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Point2D pt2D = (Point2D) o;

        if (Double.compare(pt2D.x, x) != 0) return false;
        return Double.compare(pt2D.y, y) == 0;
    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        temp = Double.doubleToLongBits(x);
        result = (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(y);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        return result;
    }

    public Point2D(double x, double y) {
        this.x = x;
        this.y = y;
    }
}
