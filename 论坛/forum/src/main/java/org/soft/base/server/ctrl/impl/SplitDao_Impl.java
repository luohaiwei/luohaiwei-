package org.soft.base.server.ctrl.impl;

import org.soft.base.server.ctrl.dao.SplitDao;
import org.springframework.stereotype.Controller;

@Controller("splitDao")
public class SplitDao_Impl implements SplitDao {
    @Override
    public int allPages(int rows, int size) {
        int i = rows / size;
        if(rows %  size != 0){
            i += 1;
        }
        return i;
    }

    @Override
    public int getBegin(int current, int size) {
        int i = (current - 1) * size;
        return i;
    }
}
