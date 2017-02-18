package xyz.thelostsoul.dao;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import xyz.thelostsoul.bean.User;

import java.util.List;

/**
 * Created by jamie on 17-2-13.
 */
@Mapper
public interface UserMapper {
    @Select("select * from user where id=#{id}")
    User selectByPrimaryKey(@Param("id") int id);

    @Select("select id,name from user")
    List<User> allUsers();

    @Insert("insert into user(name,password) value(#{name},#{password})")
    int insertUser(User user);
}
