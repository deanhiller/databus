package nosql.util;

import java.util.Iterator;

import com.alvazan.orm.api.z8spi.iter.Cursor;

public class IterableCursorWrapper<T> implements Iterable {

	Cursor<T> cursor;
	Iterator<T> theIterator = null;
	
	public IterableCursorWrapper(Cursor<T> underlyingCursor) {
		cursor = underlyingCursor;
		theIterator = new CursorIterator<T>(cursor);
	}
	@Override
	public Iterator iterator() {
		return theIterator;
	}
	
	private static class CursorIterator<T> implements Iterator {

		private Cursor<T> cursor;
		private boolean nextCalled = false;
		private boolean hasNext = true;
		
		public CursorIterator(Cursor<T> theCursor) {
			cursor = theCursor;
		}
		
		@Override
		public boolean hasNext() {
			if (!nextCalled) {
				nextCalled = true;
				hasNext = cursor.next();
			}
			return hasNext;
				
		}

		@Override
		public T next() {
			if (!nextCalled) 
				next();
			nextCalled=false;
			T ret = cursor.getCurrent();
			//jsc:todo  this is messed up.  Force a cache load till I figure out a better way to do this.
			if (ret != null)
				ret.toString();
			return ret;
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException("IterableCursorWrapper does not currently support remove.");
			
		}
		
	}
}
