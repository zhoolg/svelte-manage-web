package com.zhoolg.manage.controller.web;

import com.zhoolg.manage.service.IAuthService;
import com.zhoolg.manage.service.IAdminUserService;
import com.zhoolg.manage.service.ICrudService;
import com.zhoolg.manage.entity.base.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/web/properties")
public class WebPropertyController {

    @GetMapping
    public ApiResponse<Map<String, Object>> list(@RequestParam(defaultValue = "1") int pageNum,
                                                 @RequestParam(defaultValue = "10") int pageSize) {
        List<Map<String, Object>> properties = List.of(
                property(1, "阳光花园 2 室 1 厅", 3200, "朝阳区", List.of("近地铁", "可短租")),
                property(2, "城市公寓 1 室", 2600, "海淀区", List.of("精装修", "拎包入住"))
        );
        return ApiResponse.ok(Map.of("list", properties, "total", properties.size(), "pageNum", pageNum, "pageSize", pageSize));
    }

    @GetMapping("/{id}")
    public ApiResponse<Map<String, Object>> detail(@PathVariable long id) {
        return ApiResponse.ok(property(id, "阳光花园 2 室 1 厅", 3200, "朝阳区", List.of("近地铁", "可短租")));
    }

    private Map<String, Object> property(long id, String title, int price, String district, List<String> tags) {
        return Map.of(
                "id", id,
                "title", title,
                "price", price,
                "district", district,
                "area", 68,
                "images", List.of(),
                "tags", tags
        );
    }
}
