package rstParser;

public class IntPair {
	private Integer first;
	private Integer second;
	
	public IntPair(Integer first, Integer second) {
		this.first = first;
		this.second = second;
	}
	
	public Integer getFirst(){
		return this.first;
	}
	
	public Integer getSecond(){
		return this.second;
	}
	
	public boolean equals(Object other)
	{
		if (other instanceof IntPair)
		{
			IntPair p = (IntPair)other;
			return p.first == this.first
				&& p.second == this.second;
		}
		return false;
	}
}
