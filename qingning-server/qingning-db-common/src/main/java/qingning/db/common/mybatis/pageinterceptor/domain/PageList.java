package qingning.db.common.mybatis.pageinterceptor.domain;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;

/**
 * 
 * 包含“分页”信息的List
 * 
 * <p>要得到总页数请使用 toPaginator().getTotalPages();</p>
 * 
 * @author 
 * @author 
 */
public class PageList<T> extends ArrayList<T> implements Serializable {
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private int total;
    private Paginator paginator;
    public PageList() {}
    
	public PageList(Collection<? extends T> c) {
		super(c);
	}

	
	public PageList(Collection<? extends T> c,Paginator p) {
        super(c);
        this.paginator = p;
     }
	public PageList(Paginator p) {
        this.paginator = p;
    }


	/**
	 * 得到分页器，通过Paginator可以得到总页数等值
	 * @return
	 */
	public Paginator getPaginator() {
		return paginator;
	}

	public int getTotal() {
		return total;
	}

	public void setTotal(Integer total) {
		this.total = total;
	}
}
