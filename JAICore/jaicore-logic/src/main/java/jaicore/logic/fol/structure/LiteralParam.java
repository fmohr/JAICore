package jaicore.logic.fol.structure;

import java.io.Serializable;

/**
 * The parameter of a literal.
 * 
 * @author mbunse
 */
@SuppressWarnings("serial")
public abstract class LiteralParam implements Serializable {

	private String name;
	protected Type type;

	/**
	 * @param name
	 *            The name of this parameter;
	 */
	public LiteralParam(String name) {
		this.name = name;
	}

	/**
	 * @param name
	 *            The name of this parameter;
	 */
	public LiteralParam(String name, Type type) {
		this(name);
		this.setType(type);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}

	/**
	 * It is with intention that the equals method does NOT check the type.
	 * We assume that the name of a parameter is sufficient to identify it.
	 * The type is rather optional to enable efficient processing in some contexts.
	 *  
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		LiteralParam other = (LiteralParam) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}

	public String getName() {
		return name;
	}

	public Type getType() {
		return type;
	}

	public void setType(Type type) {
		this.type = type;
	}

}
