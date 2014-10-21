package models;

import com.alvazan.orm.api.base.CursorToMany;

public abstract class RemoveSafeCursor<T> implements CursorToMany<T> {
	private CursorToMany<T> delegate = null;

	public RemoveSafeCursor(CursorToMany<T> underlyingCursor) {
		delegate = underlyingCursor;
	}
	@Override
	public void beforeFirst() {
		delegate.beforeFirst();
	}

	@Override
	public boolean next() {
		boolean delegateHasNext = delegate.next();
		if (delegateHasNext) {
			if (isDeleted(delegate.getCurrent())) {
				delegate.removeCurrent();
				return next();
			}
			return true;
		}
		return false;
	}

	protected abstract boolean isDeleted(Object next);
	
	@Override
	public T getCurrent() {
		return delegate.getCurrent();
	}

	@Override
	public void afterLast() {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean previous() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void removeCurrent() {
		// TODO Auto-generated method stub

	}

	@Override
	public void addElement(T element) {
		// TODO Auto-generated method stub

	}

}
