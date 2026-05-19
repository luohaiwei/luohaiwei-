package org.soft.base.server.ctrl;

import org.soft.base.model.Users;
import org.soft.base.server.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class TO {

    @RequestMapping("/to/page/{p1}")
    public String toPage(@PathVariable("p1") String p1){
        return p1;
    }

    @RequestMapping("/")
    public String toLogin(){
        return "login";
    }
}
