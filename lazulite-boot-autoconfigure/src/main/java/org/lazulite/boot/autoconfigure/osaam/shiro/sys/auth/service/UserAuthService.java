/*
 * Copyright 2016. junfu
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 */
package org.lazulite.boot.autoconfigure.osaam.shiro.sys.auth.service;


import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.Sets;
import org.apache.commons.lang3.StringUtils;
import org.lazulite.boot.autoconfigure.core.utils.SpringUtils;
import org.lazulite.boot.autoconfigure.osaam.shiro.sys.group.service.GroupService;
import org.lazulite.boot.autoconfigure.osaam.shiro.sys.organization.service.JobService;
import org.lazulite.boot.autoconfigure.osaam.shiro.sys.organization.service.OrganizationService;
import org.lazulite.boot.autoconfigure.osaam.shiro.sys.permission.entity.Permission;
import org.lazulite.boot.autoconfigure.osaam.shiro.sys.permission.entity.Role;
import org.lazulite.boot.autoconfigure.osaam.shiro.sys.permission.entity.RoleResourcePermission;
import org.lazulite.boot.autoconfigure.osaam.shiro.sys.permission.service.PermissionService;
import org.lazulite.boot.autoconfigure.osaam.shiro.sys.permission.service.RoleService;
import org.lazulite.boot.autoconfigure.osaam.shiro.sys.resource.entity.Resource;
import org.lazulite.boot.autoconfigure.osaam.shiro.sys.resource.service.ResourceService;
import org.lazulite.boot.autoconfigure.osaam.shiro.sys.user.entity.User;
import org.lazulite.boot.autoconfigure.osaam.shiro.sys.user.entity.UserOrganizationJob;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * 分组、组织机构、用户、新增、修改、删除时evict缓存
 * <p>
 * 获取用户授权的角色及组装好的权限
 */
@Service
public class UserAuthService {

    @Autowired
    private GroupService groupService;

    @Autowired
    private OrganizationService organizationService;

    @Autowired
    private JobService jobService;

    @Autowired
    private AuthService authService;

    @Autowired
    private RoleService roleService;

    @Autowired
    private ResourceService resourceService;

    @Autowired
    private PermissionService permissionService;


    public Set<Role> findRoles(User user) {

        if (user == null) {
            return Sets.newHashSet();
        }

        Long userId = user.getId();

        Set<Long[]> organizationJobIds = Sets.newHashSet();
        Set<Long> organizationIds = Sets.newHashSet();
        Set<Long> jobIds = Sets.newHashSet();

        for (UserOrganizationJob o : user.getOrganizationJobs()) {
            Long organizationId = o.getOrganizationId();
            Long jobId = o.getJobId();

            if (organizationId != null && jobId != null && organizationId != 0L && jobId != 0L) {
                organizationJobIds.add(new Long[]{organizationId, jobId});
            }
            organizationIds.add(organizationId);
            jobIds.add(jobId);
        }

        //TODO 目前默认子会继承父 后续实现添加flag控制是否继承

        //找组织机构祖先
        //organizationIds.addAll(organizationService.findAncestorIds(organizationIds));
        //找工作职务的祖先
        // jobIds.addAll(jobService.findAncestorIds(jobIds));

        //过滤组织机构 仅获取目前可用的组织机构数据
        organizationService.filterForCanShow(organizationIds, organizationJobIds);
        jobService.filterForCanShow(jobIds, organizationJobIds);

        //过滤工作职务 仅获取目前可用的工作职务数据

        //默认分组 + 根据用户编号 和 组织编号 找 分组
        Set<Long> groupIds = groupService.findShowGroupIds(userId, organizationIds);

        //获取权限
        //1.1、获取用户角色
        //1.2、获取组织机构角色
        //1.3、获取工作职务角色
        //1.4、获取组织机构和工作职务组合的角色
        //1.5、获取组角色
        Set<Long> roleIds = authService.findRoleIds(userId, groupIds, organizationIds, jobIds, organizationJobIds);

        Set<Role> roles = roleService.findShowRoles(roleIds);

        return roles;

    }

    public Set<String> findStringRoles(User user) {

        Set<Role> roles = ((UserAuthService) SpringUtils.getBean(UserAuthService.class)).findRoles(user);
        return Sets.newHashSet(Collections2.transform(roles, new Function<Role, String>() {
            @Override
            public String apply(Role input) {
                return input.getRole();
            }
        }));
    }

    /**
     * 根据角色获取 权限字符串 如sys:admin
     *
     * @param user
     * @return
     */
    public Set<String> findStringPermissions(User user) {
        Set<String> permissions = Sets.newHashSet();

        Set<Role> roles = ((UserAuthService) SpringUtils.getBean(UserAuthService.class)).findRoles(user);
        for (Role role : roles) {
            for (RoleResourcePermission rrp : role.getResourcePermissions()) {
                Resource resource = resourceService.findOne(rrp.getResourceId());

                String actualResourceIdentity = resourceService.findActualResourceIdentity(resource);

                //不可用 即没查到 或者标识字符串不存在
                if (resource == null || StringUtils.isEmpty(actualResourceIdentity)) {
                    continue;
                }

                for (Long permissionId : rrp.getPermissionIds()) {
                    Permission permission = permissionService.findOne(permissionId);

                    //不可用
                    if (permission == null || Boolean.FALSE.equals(permission.getShow())) {
                        continue;
                    }
                    permissions.add(actualResourceIdentity + ":" + permission.getPermission());

                }
            }

        }

        return permissions;
    }

}
