package seed.dataset;

public abstract class DatasetElement {
	public int xDim;
	public int yDim;
	
	public void setDim(int xDim, int yDim) {
		this.xDim = xDim;
		this.yDim = yDim;
	}
	abstract public SampleVector getVector();
}
