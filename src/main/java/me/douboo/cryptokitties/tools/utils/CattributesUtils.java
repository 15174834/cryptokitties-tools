package me.douboo.cryptokitties.tools.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

public class CattributesUtils {

	private final static Logger logger = LoggerFactory.getLogger(CattributesUtils.class);
	public static final ConcurrentMap<String, Integer> cattributesMap = new ConcurrentHashMap<String, Integer>();

	public static int calcSum(String cattributes) {
		if (CollectionUtils.isEmpty(cattributesMap) || StringUtils.isEmpty(cattributes))
			return 0;
		String[] catts = cattributes.split(",");
		int sum = 0;
		if (null != catts && catts.length > 0) {
			for (String catt : catts) {
				sum += cattributesMap.get(catt);
			}
		}
		return sum;
	}

	public static int calcAvg(String cattributes) {
		if (CollectionUtils.isEmpty(cattributesMap))
			return 0;
		String[] catts = cattributes.split(",");
		int sum = calcSum(cattributes);
		int avg = sum / catts.length;
		return avg;
	}

	public static List<Entry<String, Integer>> sort() {
		// 排序
		List<Map.Entry<String, Integer>> list = new ArrayList<Map.Entry<String, Integer>>(cattributesMap.entrySet());
		Collections.sort(list, new Comparator<Map.Entry<String, Integer>>() {
			@Override
			public int compare(Map.Entry<String, Integer> o1, Map.Entry<String, Integer> o2) {
				// 升序排序
				return o1.getValue().compareTo(o2.getValue());
			}
		});
		if (logger.isTraceEnabled())
			for (Entry<String, Integer> mapping : list) {
				logger.trace("key:" + mapping.getKey() + "  value:" + mapping.getValue());
			}
		return list;

	}

	public static JSONArray makeTag(String cattributes) {
		JSONArray array = new JSONArray();
		if (CollectionUtils.isEmpty(cattributesMap))
			return array;
		List<Entry<String, Integer>> sort = sort();
		String[] catts = cattributes.split(",");

		// 遍历
		if (ArrayUtils.isNotEmpty(catts))
			for (int i = 0; i < catts.length; i++) {
				for (int j = 0; j < sort.size(); j++) {
					if (sort.get(j).getKey().equals(catts[i])) {
						JSONObject json = new JSONObject();
						json.put("attr", catts[i]);
						json.put("num", sort.get(j).getValue());
						json.put("ranking", j + 1); // 排行
						array.add(json);
					}
				}
			}
		return array;
	}

}