package cn.pcorp.controllor;

import java.io.IOException;
import java.text.ParseException;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;

import cn.pcorp.controllor.util.MethodConstant;
import cn.pcorp.dao.BaseDao;
import cn.pcorp.dao.MainDao;
import cn.pcorp.impl.sys.ServiceListener;
import cn.pcorp.model.DynaBean;
import cn.pcorp.service.BaseService;
import cn.pcorp.service.system.SysServer;
import cn.pcorp.util.ApiUtil;
import cn.pcorp.util.BeanUtils;
import cn.pcorp.util.PageUtil;
import cn.pcorp.util.SyConstant;
import javacommon.util.JsonUtils;

/**
 * @author panlihai E-mail:18611140788@163.com
 * @version 创建时间：2015年12月4日 上午10:54:45 类说明: 对平台处理操作
 */
@RestController()
public class MainController {
	@Resource(name = "baseService")
	private BaseService baseService;
	private static org.apache.log4j.Logger logger = Logger.getLogger(MainController.class);

	/**
	 * 对APPID进行操作显示 get请求只作显示操作
	 * 
	 * @param appId
	 * @param action
	 * @param request
	 * @version SUPVISOR
	 * @return
	 * @throws ParseException
	 */
	@RequestMapping(value = "/api/{pId}/{sId}/{action}", method = { RequestMethod.GET, RequestMethod.POST,
			RequestMethod.PUT, RequestMethod.DELETE })
	@ResponseBody
	public void impl(@PathVariable String pId, @PathVariable String sId, @PathVariable String action,
			HttpServletRequest request, HttpServletResponse response) throws ParseException {
		MainDao dao = baseService.getMainDao();
		// 返回结果
		DynaBean result = null;
		// 校验合法性
		ResponseModel rs = null;
		// 获取参数列表信息
		DynaBean paramBean = BeanUtils.requestToDynaBean(request);
		// 数据源名称编码
		paramBean.setStr(BeanUtils.KEY_DATASOURCEKEY, "");
		// 获取操作参数
		paramBean.set(MethodConstant.ACT, action);
		// 获取产品id
		paramBean.set(MethodConstant.PID, pId);
		// 获取应用id
//		paramBean.set(MethodConstant.SID, sId);
		//
		paramBean.setStr(PageUtil.PAGE_APPID, paramBean.getStr(MethodConstant.AID));
		// 获取用户信息
		try {
			paramBean.set("USERINFO", baseService.getUserInfo(baseService.getMainDao(), null, paramBean));
			// // 得到请求对象
			RequestModel rm = this.baseService.getRequestModel(paramBean, request, response);
			if (rm == null) {
				result = this.baseService.getBackCode(rm, null, "40004", ApiUtil.getBackName(baseService.getMainDao(),baseService.getBaseDao(), "40004"), paramBean,
						"校验异常:校验产品的服务是否开放");
			} else {
				// 校验参数合法性
				rs = ApiUtil.checkParams(rm);
				if (!rs.getCode().equals("0")) {
					result = this.baseService.getBackCode(rm, rs, null, null, paramBean,
							"校验异常:校验请求参数是否合法,请参考:" + rs.getMsg());
				} else {
					if (paramBean.getStr("COMPOSITE", "").length() == 0) {
						rs = this.baseService.doAction(rm);
					} else {
						rs = baseService.doSomeAct(rm);
					}
					result = this.baseService.getBackCode(rm, rs, null, null, paramBean, "");
				}
			}
//			当返回rs为空是自定义返回，rm为空时则未提供服务，rs不为空是平台实现的返回。
			if (rm == null ||  rs != null) {
				// UTF-8编码
				response.setCharacterEncoding("UTF-8");
				// 把结果写回响应中
				response.getWriter().write(JSON.toJSONString(result.getValues(), SerializerFeature.WriteMapNullValue));
				// 刷新
				response.getWriter().flush();
				logger.debug(result.toJsonString());
			}
		} catch (Exception e) {
			logger.error(e.getMessage());
			e.printStackTrace();
		}
	}

	/**
	 * 对APPCODE进行操作get请求只作功能操作
	 * 
	 * @param appCode
	 * @param action
	 * @param request
	 * @return
	 * @throws ParseException
	 */
	@RequestMapping(value = "/ajax/{pId}/{menuId}/{appId}/{action}", method = RequestMethod.GET)
	@ResponseBody
	public void getMenuAjax(@PathVariable String pId, @PathVariable String menuId, @PathVariable String appId,
			@PathVariable String action, HttpServletRequest request, HttpServletResponse response)
			throws ParseException {
		String json = "";
		try {
			DynaBean paramBean = BeanUtils.requestToDynaBean(request);
			// 获取用户信息
			paramBean.set("USERINFO", baseService.getUserInfo(baseService.getMainDao(), null, paramBean));
			// 数据源名称编码
			paramBean.setStr("KEY", "");
			paramBean.set(PageUtil.PAGE_ACTION, action);
			paramBean.set(PageUtil.PAGE_APPID, appId);
			paramBean.set(PageUtil.PAGE_MENUID, menuId.equals("TOP") ? "" : menuId);
			paramBean.set(PageUtil.PAGE_PID, pId);
			switch (action) {
			case SyConstant.ACT_VIEW_MENUS:
				json = JsonUtils.toJson(baseService.showMenus(paramBean).getModel());
				break;
			/** ACT_DATA_MENUS 显示所有的数据 */
			case SyConstant.ACT_VIEW_ONE:
				json = JsonUtils.toJson(baseService.viewOne(paramBean).getModel());
				break;
			/** $ACTION$：显示列表查看页面 */
			case SyConstant.ACT_LIST_VIEW:
				json = JsonUtils.toJson(baseService.listView(paramBean).getModel());
				break;
			/** $ACTION$：显示列表JSON数据 */
			case SyConstant.ACT_DATA_JSON:
				// 根据应用程序获得json
				json = baseService.listJsonFromAppid(paramBean.getStr(BeanUtils.KEY_DATASOURCEKEY), appId);
				break;
			/** $ACTION$：获取静态数据字典列表列表JSON数据 */
			case SyConstant.ACT_DATA_JSON_VALUE:
				json = baseService.listJsonValueByDicId(appId);
				break;
			/** $ACTION$：显示添加页面 */
			case SyConstant.ACT_CARD_ADD:
				/** $ACTION$：显示列表编辑页面 */
			case SyConstant.ACT_LIST_EDIT:
			case SyConstant.ACT_LIST_ADD:
				json = JsonUtils.toJson(baseService.cardAdd(paramBean).getModel());
				break;
			/** $ACTION$：卡片保存 */
			case SyConstant.ACT_CARD_SAVE:
				json = JsonUtils.toJson(baseService.cardSave(paramBean).getModel());
				break;
			/** $ACTION$：删除 */
			case SyConstant.ACT_DELETE:
				json = JsonUtils.toJson(baseService.listDelete(paramBean).getModel());
				break;
			default: // 执行自定义的操作
				paramBean.setStr(PageUtil.PAGE_APPID, paramBean.getStr(MethodConstant.AID));
				Object lstn = SysServer.getServer().getBean(appId);
				if (lstn == null) {
					lstn = SysServer.getServer().getBean("PARENTSERVICE");
				}
				ServiceListener listener = (ServiceListener) lstn;
				// 获取用户信息
				paramBean.set("USERINFO", baseService.getUserInfo(baseService.getMainDao(), null, paramBean));
				json = JsonUtils.toJson(listener.execute(request, response, baseService.getMainDao(), paramBean));
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		try {
			response.setCharacterEncoding("UTF-8");
			response.getWriter().write(json);
			logger.debug(json);
			response.getWriter().flush();
		} catch (IOException e) {
			logger.error(e.getMessage());
			e.printStackTrace();
		}
	}

	public void clearCache() {
		baseService.clearCache();
	}

	/**
	 * 对APPID进行操作显示 get请求只作显示操作
	 * 
	 * @param appId
	 * @param action
	 * @param request
	 * @return
	 * @throws ParseException
	 */
	@RequestMapping(value = "/view/{pId}/{appId}/{action}", method = RequestMethod.GET)
	public ModelAndView view(@PathVariable String pId, @PathVariable String appId, @PathVariable String action,
			HttpServletRequest request) throws ParseException {
		ModelAndView view = null;
		HttpSession session = request.getSession();
		try {
			DynaBean paramBean = BeanUtils.requestToDynaBean(request);
			paramBean.set(PageUtil.PAGE_ACTION, action);
			paramBean.set(PageUtil.PAGE_APPID, appId);
			paramBean.set(PageUtil.PAGE_PID, pId);
			switch (action) {
			/** $ACTION$：显示列表查看页面 */
			case SyConstant.ACT_LIST_VIEW:
				return new ModelAndView("/view");
			/** $ACTION$：显示添加页面 */
			case SyConstant.ACT_CARD_ADD:
				return new ModelAndView("/view");
			}
		} catch (Exception e) {
			e.printStackTrace();
			view = new ModelAndView("/exception");
		}
		return view;
	}

}