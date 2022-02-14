package seed.util;

import java.util.ArrayList;
import java.util.List;

public class RingBuffer<T> {
	public int start = 0;
	public int end = 0;
	public List<T> buf;
	public void setBuffer(int size) {
		buf = new ArrayList<>(size);
		for (int i = 0;i < size;i++) {
			buf.add(null);
		}
	}
	
	public void add(T elm) {
		buf.set(end, elm);
		end = (end + 1) % buf.size();
		if (end == start) {
			start = (start + 1) % buf.size();
		}
	}
}
