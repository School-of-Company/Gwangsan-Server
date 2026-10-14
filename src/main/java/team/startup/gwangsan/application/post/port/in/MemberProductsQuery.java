package team.startup.gwangsan.application.post.port.in;

import team.startup.gwangsan.application.post.data.MemberProductsCriteria;
import team.startup.gwangsan.application.post.data.ProductView;

import java.util.List;

public interface MemberProductsQuery {
    List<ProductView> query(MemberProductsCriteria criteria);
}
