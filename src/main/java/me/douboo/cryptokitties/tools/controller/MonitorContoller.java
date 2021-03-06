package me.douboo.cryptokitties.tools.controller;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.alibaba.fastjson.JSONObject;

import me.douboo.cryptokitties.tools.task.CommanderJob;

@Controller
@RequestMapping
public class MonitorContoller {

	@GetMapping({ "", "/" })
	public String index() {
		return "index";
	}

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@RequestMapping(path = "/data")
	@ResponseBody
	public JSONObject data(HttpServletRequest request, String search, @RequestParam(required = false, defaultValue = "0") Integer offset, @RequestParam(required = false, defaultValue = "50") Integer limit, String sort, String order) {
		request.setAttribute("connections", CommanderJob.commandQueue1.size());
		JSONObject page = new JSONObject();
		List<Object> params = new ArrayList<>();
		String sql = "SELECT count(1) FROM t_auction a LEFT JOIN t_kitty k  ON a.`kitty_id` = k.`id` ";
		if (StringUtils.isNotEmpty(search)) {
			sql += " where k.id = ?";
			params.add(search);
		}
		int total = jdbcTemplate.queryForObject(sql, params.toArray(), Integer.class);
		page.put("total", total);

		params.clear();

		sql = "SELECT k.`status_cooldown_index`,k.`image_url`,k.`bio`,a.`current_price`,k.`id`,k.`kitty_name`,k.`generation`,k.`children_num`,k.`cattributes_sum`,k.`cattributes_avg`,k.`cattributes`,k.`cattributes_tags` FROM t_auction a LEFT JOIN t_kitty k  ON a.`kitty_id` = k.`id`  ";

		if (StringUtils.isNotEmpty(search)) {
			sql += " where k.id = ?";
			params.add(search);
		}

		if ("current_price".equals(sort)) {
			sql += "order by ? ?";
			params.add(sort);
			params.add(order);
		} else {
			sql += "order by ? ?,current_price asc";
			params.add(sort);
			params.add(order);
		}
		sql += " limit ?,? ";
		params.add(offset);
		params.add(limit);

		List<JSONObject> rows = jdbcTemplate.query(sql, params.toArray(), new RowMapper<JSONObject>() {
			@Override
			public JSONObject mapRow(ResultSet rs, int rowNum) throws SQLException {
				JSONObject item = new JSONObject();
				item.put("id", rs.getString("id"));
				item.put("current_price", rs.getString("current_price"));
				item.put("kitty_name", rs.getString("kitty_name"));
				item.put("generation", rs.getString("generation"));
				item.put("children_num", rs.getString("children_num"));
				item.put("cattributes_sum", rs.getString("cattributes_sum"));
				item.put("cattributes_avg", rs.getString("cattributes_avg"));
				item.put("cattributes", rs.getString("cattributes"));
				item.put("cattributes_tags", rs.getString("cattributes_tags"));
				item.put("image_url", rs.getString("image_url"));
				item.put("bio", rs.getString("bio"));
				item.put("status_cooldown_index", rs.getString("status_cooldown_index"));
				return item;
			}
		});
		page.put("rows", rows);
		return page;
	}
}
