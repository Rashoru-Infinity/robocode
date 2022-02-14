package seed.predmodel;

import seed.dataset.DatasetElement;
import seed.util.RingBuffer;

public class Regressor extends Thread {
	protected RingBuffer<DatasetElement> sampleBuf;
	protected double[] bias;
	protected double[][] weight;
	protected double learningRate = 0.001;
	protected int epoch = 10;
	protected int batchSize;
	protected int batchStart = 0;

	public Regressor(int sampleBufSize, int xDimension, int degree) {
		sampleBuf = new RingBuffer<>();
		sampleBuf.setBuffer(sampleBufSize);
		bias = new double[xDimension];
		weight = new double[degree][xDimension];
		batchSize = sampleBufSize;
		for (int i = 0;i < xDimension;i++) {
			bias[i] = Math.random();
			for (int deg = 0;deg < degree;deg++) {
				weight[deg][i] = Math.random();
			}
		}
	}
	
	public Regressor(int sampleBufSize, int batchSize, int xDimension, int degree) {
		sampleBuf = new RingBuffer<>();
		sampleBuf.setBuffer(sampleBufSize);
		bias = new double[xDimension];
		weight = new double[degree][xDimension];
		this.batchSize = batchSize <= sampleBufSize ? batchSize : sampleBufSize;
		for (int i = 0;i < xDimension;i++) {
			bias[i] = Math.random();
			for (int deg = 0;deg < degree;deg++) {
				weight[deg][i] = Math.random();
			}
		}
	}
	
	public void addSample(DatasetElement sample) {
		if (sample == null) {
			return ;
		}
		sampleBuf.add(sample);
	}
	
	/* Regress */
	public void updateModel() {
		if (sampleBuf.start == sampleBuf.end) {
			return ;
		}
		int start = sampleBuf.start;
		int end = sampleBuf.end;
		int availableSize = end > start ? end - start : sampleBuf.buf.size() + (end - start);
		for (int dim = 0;dim < bias.length;dim++) {
			for (int step = 0;step < epoch;step++) {
				double sigma = 0.0;
				for (int i = 0;i < availableSize && i < batchSize;i++) {
					DatasetElement dataset = sampleBuf.buf.get((batchStart + i) % batchSize);
					if (dataset == null) {
						return ;
					}
					double fx = calcFunc(dataset.getVector().x);
					sigma += fx - dataset.getVector().y[0];
				}
				sigma *= learningRate;
				bias[dim] -= sigma;
			}
		}
		for (int dim = 0;dim < bias.length;dim++) {
			for (int degree = 1;degree < weight.length + 1;degree++) {
				for (int step = 0;step < epoch;step++) {
					double sigma = 0.0;
					for (int i = 0;i < availableSize && i < batchSize;i++) {
						DatasetElement dataset = sampleBuf.buf.get((batchStart + i) % batchSize);
						if (dataset == null) {
							return ;
						}
						double fx = calcFunc(dataset.getVector().x);
						sigma += (fx - dataset.getVector().y[0]) * Math.pow(dataset.getVector().x[dim], degree);
					}
					sigma *= learningRate;
					weight[degree - 1][dim] -= sigma;
				}
			}
		}
		if (availableSize > batchSize) {
			batchStart = batchStart + batchSize <= end ? batchStart + batchSize : start + (batchStart + batchSize - end - 1);
		}
	}
	
	protected double calcFunc(double[] x) {
		double value = 0.0;
		for (int dim = 0;dim < bias.length;dim++) {
			value += bias[dim];
		}
		
		for (int degree = 1;degree <= weight.length;degree++) {
			for (int dim = 0;dim < weight[degree - 1].length;dim++) {
				value += weight[degree - 1][dim] * Math.pow(x[dim], degree);
			}
		}
		return value;
	}
	
	public double predict(DatasetElement dataset) {
		if (dataset == null) {
			return 0.0;
		}
		if (dataset.getVector().x == null) {
			return 0.0;
		}
		return calcFunc(dataset.getVector().x);
	}
	
	public void printWeight() {
		System.out.print("[");
		for (int dim = 0;dim < bias.length;dim++) {
			System.out.print(bias[dim]);
			if (dim + 1 < bias.length) {
				System.out.print(",");
			} else {
				System.out.println(",");
			}
		}
		for (int degree = 1;degree <= weight.length;degree++) {
			for (int dim = 0;dim < weight[degree - 1].length;dim++) {
				System.out.print(weight[degree - 1][dim]);
				if (dim + 1 < weight[degree - 1].length) {
					System.out.print(",");
				} else if (degree != weight.length) {
					System.out.println(",");
				}
			}
		}
		System.out.println("]");
	}
	
	public void printMSE() {
		if (sampleBuf.end == sampleBuf.start) {
			return ;
		}
		int start = sampleBuf.start;
		int end = sampleBuf.end;
		int availableSize = end > start ? end - start : sampleBuf.buf.size() + (end - start);
		double mse = 0.0;
		for (int i = 0;i < availableSize;i++) {
			DatasetElement dataset = sampleBuf.buf.get((start + i) % sampleBuf.buf.size());
			mse += Math.pow(dataset.getVector().y[0] - predict(dataset), 2);
		}
		mse /= availableSize;
		System.out.println(mse);
	}
}
