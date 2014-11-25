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
		delegate.afterLast();

	}

	@Override
	public boolean previous() {
		boolean delegateHasPrev = delegate.previous();
		if (delegateHasPrev) {
			if (isDeleted(delegate.getCurrent())) {
				delegate.removeCurrent();
				return previous();
			}
			return true;
		}
		return false;
	}

	@Override
	public void removeCurrent() {
		delegate.removeCurrent();

	}

	@Override
	public void addElement(T element) {
		delegate.addElement(element);

	}

}
