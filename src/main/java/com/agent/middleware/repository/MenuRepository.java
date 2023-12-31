package com.agent.middleware.repository;


import com.agent.middleware.entity.Menu;
import com.agent.middleware.enums.UserType;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface MenuRepository extends CrudRepository<Menu, Long> {

    List<Menu> findAllByUserType(UserType userType);
}
