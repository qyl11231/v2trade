/**
 * 公共JS工具
 */

// 获取当前登录用户
function getCurrentUser() {
    const userStr = localStorage.getItem('currentUser');
    return userStr ? JSON.parse(userStr) : null;
}

// 设置当前登录用户
function setCurrentUser(user) {
    localStorage.setItem('currentUser', JSON.stringify(user));
}

// 清除登录信息
function clearCurrentUser() {
    localStorage.removeItem('currentUser');
}

// 检查登录状态
function checkLogin() {
    const user = getCurrentUser();
    if (!user) {
        window.location.href = '/login.html';
        return false;
    }
    return true;
}

// 获取用户ID请求头
function getUserHeader() {
    const user = getCurrentUser();
    return user ? { 'X-User-Id': user.id } : {};
}

// 封装AJAX请求
function request(options) {
    const user = getCurrentUser();
    const headers = options.headers || {};
    
    if (user && user.id) {
        headers['X-User-Id'] = user.id;
    }
    
    return new Promise((resolve, reject) => {
        layui.$.ajax({
            url: options.url,
            type: options.method || 'GET',
            data: options.data ? JSON.stringify(options.data) : null,
            contentType: 'application/json',
            headers: headers,
            success: function(res) {
                if (res.code === 200) {
                    resolve(res);
                } else if (res.code === 401) {
                    clearCurrentUser();
                    window.location.href = '/login.html';
                    reject(res);
                } else {
                    layui.layer.msg(res.message || '操作失败', {icon: 2});
                    reject(res);
                }
            },
            error: function(xhr) {
                layui.layer.msg('网络错误，请稍后重试', {icon: 2});
                reject(xhr);
            }
        });
    });
}

// 退出登录
function logout() {
    clearCurrentUser();
    window.location.href = '/login.html';
}
