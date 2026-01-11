/**
 * 策略管理JS
 */
layui.use(['element', 'layer', 'form'], function(){
    var element = layui.element;
    var layer = layui.layer;
    var form = layui.form;
    var $ = layui.$;
    
    if (!checkLogin()) return;
    
    var user = getCurrentUser();
    if (user) { $('#userName').text(user.username); }
    
    var tradingPairs = [];
    var signalConfigs = [];
    var selectedTradingPairs = []; // 已选中的交易对列表
    var selectedSignalConfigs = []; // 已选中的信号配置列表
    
    var allStrategies = []; // 存储所有策略数据用于筛选
    
    // 更新统计数据
    function updateStats(strategies){
        var total = strategies.length;
        var enabled = strategies.filter(function(s){ return s.enabled == 1; }).length;
        var disabled = total - enabled;
        var signalDriven = strategies.filter(function(s){ return s.strategyType === 'SIGNAL_DRIVEN'; }).length;
        
        $('#totalStrategies').text(total);
        $('#enabledStrategies').text(enabled);
        $('#disabledStrategies').text(disabled);
        $('#signalStrategies').text(signalDriven);
    }
    
    // 筛选策略
    function filterStrategies(){
        var filterValue = $('select[name="filter"]').val();
        var keyword = ($('input[name="keyword"]').val() || '').toLowerCase();
        
        var typeFilter = '';
        var statusFilter = '';
        
        // 解析筛选值
        if(filterValue){
            if(filterValue.startsWith('type:')){
                typeFilter = filterValue.replace('type:', '');
            } else if(filterValue.startsWith('status:')){
                statusFilter = filterValue.replace('status:', '');
            }
        }
        
        var filtered = allStrategies.filter(function(item){
            var matchType = !typeFilter || item.strategyType === typeFilter;
            var matchStatus = !statusFilter || item.enabled == statusFilter;
            var matchKeyword = !keyword || item.strategyName.toLowerCase().indexOf(keyword) !== -1;
            return matchType && matchStatus && matchKeyword;
        });
        
        renderStrategyTable(filtered);
    }
    
    // 渲染策略表格
    function renderStrategyTable(strategies){
        var html = '';
        if(strategies && strategies.length > 0){
            strategies.forEach(function(item){
                var statusDot = item.enabled == 1 ? '<span class="status-dot enabled"></span><span style="color: #52c41a;">启用</span>' : '<span class="status-dot disabled"></span><span style="color: #8c8c8c;">禁用</span>';
                var typeBadge = '';
                if(item.strategyType === 'SIGNAL_DRIVEN'){
                    typeBadge = '<span class="layui-badge" style="background: #1890ff;">信号驱动</span>';
                } else if(item.strategyType === 'INDICATOR_DRIVEN'){
                    typeBadge = '<span class="layui-badge" style="background: #fa8c16;">指标驱动</span>';
                } else if(item.strategyType === 'HYBRID'){
                    typeBadge = '<span class="layui-badge" style="background: #722ed1;">混合策略</span>';
                }
                var decisionModeText = item.decisionMode || '<span style="color: #bfbfbf;">-</span>';
                var createdAt = item.createdAt ? item.createdAt.replace('T', ' ').substring(0, 16) : '-';
                
                html += '<tr>' +
                    '<td><strong>' + item.id + '</strong></td>' +
                    '<td><strong style="color: #262626;">' + item.strategyName + '</strong></td>' +
                    '<td>' + typeBadge + '</td>' +
                    '<td><span style="color: #595959; font-size: 12px;">' + decisionModeText + '</span></td>' +
                    '<td>' + statusDot + '</td>' +
                    '<td><span style="color: #8c8c8c; font-size: 12px;">' + createdAt + '</span></td>' +
                    '<td><div class="action-buttons">' +
                    '<button class="layui-btn layui-btn-xs layui-btn-normal" onclick="viewStrategyDetail(' + item.id + ')"><i class="layui-icon layui-icon-search"></i> 详情</button>' +
                    '<button class="layui-btn layui-btn-xs' + (item.enabled == 1 ? ' layui-btn-disabled' : '') + '" onclick="editStrategy(' + item.id + ', ' + item.enabled + ')"' + (item.enabled == 1 ? ' disabled title="启用中的策略不可编辑"' : '') + '><i class="layui-icon layui-icon-edit"></i> 编辑</button>' +
                    '<button class="layui-btn layui-btn-xs layui-btn-warm" onclick="toggleStrategy(' + item.id + ', ' + item.enabled + ')">' + (item.enabled == 1 ? '<i class="layui-icon layui-icon-pause"></i> 禁用' : '<i class="layui-icon layui-icon-play"></i> 启用') + '</button>' +
                    '<button class="layui-btn layui-btn-xs layui-btn-danger" onclick="deleteStrategy(' + item.id + ')"><i class="layui-icon layui-icon-delete"></i> 删除</button>' +
                    '</div></td></tr>';
            });
        } else {
            html = '<tr><td colspan="7" class="empty-state"><i class="layui-icon layui-icon-file"></i><div>暂无策略数据</div></td></tr>';
        }
        $('#strategyList').html(html);
    }
    
    // 加载策略列表
    function loadStrategies(){
        request({url: '/api/strategy/definition/list'}).then(function(res){
            allStrategies = res.data || [];
            updateStats(allStrategies); // 更新统计数据（使用全部数据）
            filterStrategies();
        });
    }
    
    // 监听筛选器变化
    form.on('select(filterAll)', function(){
        filterStrategies();
    });
    
    // 监听搜索输入
    var searchTimer;
    $('#searchKeyword').on('input', function(){
        clearTimeout(searchTimer);
        searchTimer = setTimeout(function(){
            filterStrategies();
        }, 300);
    });
    
    // 加载交易对下拉列表
    function loadTradingPairSelect(){
        request({url: '/api/trading-pair/list?enabled=1'}).then(function(res){
            tradingPairs = res.data || [];
            var html = '<option value="">请选择交易对</option>';
            if(tradingPairs.length > 0){
                tradingPairs.forEach(function(item){
                    var marketTypeText = item.marketType === 'SPOT' ? '现货' : (item.marketType === 'SWAP' ? '永续' : item.marketType);
                    html += '<option value="' + item.id + '" data-symbol="' + item.symbol + '" data-market-type="' + item.marketType + '">' + 
                        item.symbol + ' ' + marketTypeText + '</option>';
                });
            }
            $('#tradingPairSelect').html(html);
            form.render('select');
        });
    }
    
    // 加载信号配置下拉列表
    function loadSignalConfigSelect(){
        request({url: '/api/signal/config/list'}).then(function(res){
            signalConfigs = res.data || [];
            var html = '<option value="">请选择信号源</option>';
            if(signalConfigs.length > 0){
                signalConfigs.forEach(function(item){
                    html += '<option value="' + item.id + '" data-name="' + item.signalName + '">' + item.signalName + '</option>';
                });
            }
            $('#signalConfigSelect').html(html);
            form.render('select');
        });
    }
    
    // 渲染已选中的交易对表格
    function renderSelectedTradingPairs(){
        var html = '';
        if(selectedTradingPairs.length > 0){
            selectedTradingPairs.forEach(function(item, index){
                var marketTypeText = item.marketType === 'SPOT' ? '现货' : (item.marketType === 'SWAP' ? '合约' : item.marketType);
                html += '<tr data-index="' + index + '">' +
                    '<td><strong>' + (item.symbol || '-') + '</strong></td>' +
                    '<td><span class="layui-badge layui-bg-blue">' + marketTypeText + '</span></td>' +
                    '<td><input type="checkbox" name="pairEnabled_' + index + '" lay-skin="switch" lay-text="启用|禁用" ' + (item.enabled ? 'checked' : '') + ' lay-filter="pairEnabledSwitch"></td>' +
                    '<td><button type="button" class="layui-btn layui-btn-xs layui-btn-danger" onclick="removeTradingPair(' + index + ')"><i class="layui-icon layui-icon-delete"></i> 移除</button></td>' +
                    '</tr>';
            });
        } else {
            html = '<tr><td colspan="4" class="empty-state"><i class="layui-icon layui-icon-add-circle"></i><div>暂无交易对，请添加</div></td></tr>';
        }
        $('#selectedTradingPairTableBody').html(html);
        form.render('switch');
    }
    
    // 渲染已选中的信号配置表格
    function renderSelectedSignalConfigs(){
        var html = '';
        if(selectedSignalConfigs.length > 0){
            selectedSignalConfigs.forEach(function(item, index){
                html += '<tr data-index="' + index + '">' +
                    '<td><strong>' + (item.signalName || '-') + '</strong></td>' +
                    '<td><span class="layui-badge" style="background: #52c41a;">TradingView</span></td>' +
                    '<td><span style="color: #595959; font-size: 12px;">' + (item.consumeMode || 'LATEST_ONLY') + '</span></td>' +
                    '<td><input type="checkbox" name="signalEnabled_' + index + '" lay-skin="switch" lay-text="启用|禁用" ' + (item.enabled ? 'checked' : '') + ' lay-filter="signalEnabledSwitch"></td>' +
                    '<td><button type="button" class="layui-btn layui-btn-xs layui-btn-danger" onclick="removeSignalConfig(' + index + ')"><i class="layui-icon layui-icon-delete"></i> 移除</button></td>' +
                    '</tr>';
            });
        } else {
            html = '<tr><td colspan="5" class="empty-state"><i class="layui-icon layui-icon-add-circle"></i><div>暂无信号源，请添加</div></td></tr>';
        }
        $('#selectedSignalConfigTableBody').html(html);
        form.render('switch');
    }
    
    // 添加交易对
    window.addTradingPair = function(){
        var pairId = $('#tradingPairSelect').val();
        if(!pairId){
            layer.msg('请选择交易对', {icon: 2});
            return;
        }
        
        // 检查是否已添加
        var exists = selectedTradingPairs.some(function(item){
            return item.id == pairId;
        });
        if(exists){
            layer.msg('该交易对已添加', {icon: 2});
            return;
        }
        
        // 找到选中的交易对信息
        var selectedOption = $('#tradingPairSelect option:selected');
        var pair = {
            id: parseInt(pairId), // 用于去重
            tradingPairId: parseInt(pairId),
            symbol: selectedOption.data('symbol') || selectedOption.text(),
            marketType: selectedOption.data('market-type') || 'SWAP',
            enabled: true
        };
        
        selectedTradingPairs.push(pair);
        renderSelectedTradingPairs();
        
        // 清空选择
        $('#tradingPairSelect').val('');
        form.render('select');
    };
    
    // 移除交易对
    window.removeTradingPair = function(index){
        selectedTradingPairs.splice(index, 1);
        renderSelectedTradingPairs();
    };
    
    // 添加信号配置
    window.addSignalConfig = function(){
        var signalId = $('#signalConfigSelect').val();
        if(!signalId){
            layer.msg('请选择信号源', {icon: 2});
            return;
        }
        
        // 检查是否已添加（使用signalConfigId判断）
        var exists = selectedSignalConfigs.some(function(item){
            return (item.signalConfigId || item.id) == signalId;
        });
        if(exists){
            layer.msg('该信号源已添加', {icon: 2});
            return;
        }
        
        // 找到选中的信号配置信息
        var selectedOption = $('#signalConfigSelect option:selected');
        var signal = {
            id: parseInt(signalId), // 用于去重
            signalConfigId: parseInt(signalId),
            signalName: selectedOption.data('name') || selectedOption.text(),
            consumeMode: 'LATEST_ONLY',
            enabled: true
        };
        
        selectedSignalConfigs.push(signal);
        renderSelectedSignalConfigs();
        
        // 清空选择
        $('#signalConfigSelect').val('');
        form.render('select');
    };
    
    // 移除信号配置
    window.removeSignalConfig = function(index){
        selectedSignalConfigs.splice(index, 1);
        renderSelectedSignalConfigs();
    };
    
    // 监听交易对启用状态变化
    form.on('switch(pairEnabledSwitch)', function(data){
        var index = $(data.elem).closest('tr').data('index');
        if(selectedTradingPairs[index]){
            selectedTradingPairs[index].enabled = data.elem.checked;
        }
    });
    
    // 监听信号配置启用状态变化
    form.on('switch(signalEnabledSwitch)', function(data){
        var index = $(data.elem).closest('tr').data('index');
        if(selectedSignalConfigs[index]){
            selectedSignalConfigs[index].enabled = data.elem.checked;
        }
    });
    
    // 打开创建策略表单（已移动到规则构建器初始化部分）
    
    // 编辑策略（调用详情接口，复用创建策略表单）
    window.editStrategy = function(id, enabled){
        // 检查策略是否启用
        if(enabled == 1){
            layer.msg('启用中的策略不可编辑，请先禁用策略', {icon: 2, time: 3000});
            return;
        }
        
        var loading = layer.load(1);
        
        // 调用详情接口获取完整信息
        request({url: '/api/strategy/definition/detail/' + id}).then(function(res){
            layer.close(loading);
            
            if(res.data){
                var detail = res.data;
                
                // 重置已选列表
                selectedTradingPairs = [];
                selectedSignalConfigs = [];
                
                // 填充策略定义
                if(detail.definition){
                    $('#createStrategyId').val(detail.definition.id); // 设置隐藏的strategyId
                    $('input[name="strategyName"]', '#createStrategyForm').val(detail.definition.strategyName);
                    $('select[name="strategyType"]', '#createStrategyForm').val(detail.definition.strategyType);
                    $('select[name="decisionMode"]', '#createStrategyForm').val(detail.definition.decisionMode || '');
                }
                
                // 填充策略参数
                if(detail.param){
                    $('input[name="initialCapital"]', '#createStrategyForm').val(detail.param.initialCapital);
                    $('input[name="baseOrderRatio"]', '#createStrategyForm').val(detail.param.baseOrderRatio);
                    $('input[name="takeProfitRatio"]', '#createStrategyForm').val(detail.param.takeProfitRatio || '');
                    $('input[name="stopLossRatio"]', '#createStrategyForm').val(detail.param.stopLossRatio || '');
                    
                    // 设置条件JSON并初始化构建器
                    var entryCondition = detail.param.entryCondition || '{"version":"1.0","mode":"ALL","rules":[],"groups":[]}';
                    var exitCondition = detail.param.exitCondition || '{"version":"1.0","mode":"ALL","rules":[],"groups":[]}';
                    $('#entryConditionJson').val(entryCondition);
                    $('#exitConditionJson').val(exitCondition);
                }
                
                // 填充交易对列表
                if(detail.tradingPairs && detail.tradingPairs.length > 0){
                    detail.tradingPairs.forEach(function(item){
                        selectedTradingPairs.push({
                            id: item.tradingPairId, // 注意：这里使用tradingPairId作为id，用于去重
                            tradingPairId: item.tradingPairId,
                            symbol: item.tradingPairName || '',
                            marketType: item.marketType || 'SWAP',
                            enabled: item.enabled == 1,
                            strategySymbolId: item.id // 保存策略交易对的ID，用于更新
                        });
                    });
                }
                
                // 填充信号订阅列表
                if(detail.signalSubscriptions && detail.signalSubscriptions.length > 0){
                    detail.signalSubscriptions.forEach(function(item){
                        selectedSignalConfigs.push({
                            id: item.signalConfigId, // 注意：这里使用signalConfigId作为id，用于去重
                            signalConfigId: item.signalConfigId,
                            signalName: item.signalConfigName || '',
                            consumeMode: item.consumeMode || 'LATEST_ONLY',
                            enabled: item.enabled == 1,
                            strategySubscriptionId: item.id // 保存策略订阅的ID，用于更新
                        });
                    });
                }
                
                // 加载下拉列表
                loadTradingPairSelect();
                loadSignalConfigSelect();
                renderSelectedTradingPairs();
                renderSelectedSignalConfigs();
                
                // 初始化条件构建器
                initConditionBuilder('entryConditionBuilder', 'entryConditionJson');
                initConditionBuilder('exitConditionBuilder', 'exitConditionJson');
                
                // 打开编辑弹窗（复用创建策略表单）
                layer.open({
                    type: 1,
                    title: '编辑策略',
                    area: ['1400px', '95%'],
                    content: $('#createStrategyForm'),
                    success: function(layero, index){
                        form.render();
                        setTimeout(function(){
                            form.render('select');
                        }, 50);
                    }
                });
            } else {
                layer.msg('获取策略详情失败', {icon: 2});
            }
        }).catch(function(err){
            layer.close(loading);
            layer.msg('获取策略详情失败: ' + (err.message || '未知错误'), {icon: 2});
        });
    };
    
    // 切换策略启用状态
    window.toggleStrategy = function(id, currentStatus){
        var newStatus = currentStatus == 1 ? 0 : 1;
        var statusText = newStatus == 1 ? '启用' : '禁用';
        layer.confirm('确定要' + statusText + '该策略吗？', function(index){
            request({
                url: '/api/strategy/definition/' + id,
                method: 'PUT',
                data: {enabled: newStatus}
            }).then(function(res){
                layer.msg(res.message, {icon: 1});
                loadStrategies();
                layer.close(index);
            });
        });
    };
    
    // 删除策略
    window.deleteStrategy = function(id){
        layer.confirm('确定删除该策略吗？删除后相关配置将一并删除。', function(index){
            request({
                url: '/api/strategy/definition/' + id,
                method: 'DELETE'
            }).then(function(res){
                layer.msg(res.message, {icon: 1});
                loadStrategies();
                layer.close(index);
            });
        });
    };
    
    // 创建/编辑策略表单提交（事务性）
    form.on('submit(submitCreateStrategy)', function(data){
        var loading = layer.load(1);
        var formData = data.field;
        var strategyId = $('#createStrategyId').val();
        var isEdit = strategyId && strategyId.trim() !== '';
        
        // 使用已选中的交易对列表
        var selectedPairs = selectedTradingPairs.map(function(item){
            var pair = {
                tradingPairId: item.tradingPairId || item.id,
                enabled: item.enabled ? 1 : 0
            };
            // 如果是编辑模式且有strategySymbolId，则添加id用于更新
            if(isEdit && item.strategySymbolId){
                pair.id = item.strategySymbolId;
            }
            return pair;
        });
        
        // 使用已选中的信号配置列表
        var selectedSignals = selectedSignalConfigs.map(function(item){
            var signal = {
                signalConfigId: item.signalConfigId || item.id,
                consumeMode: item.consumeMode || 'LATEST_ONLY',
                enabled: item.enabled ? 1 : 0
            };
            // 如果是编辑模式且有strategySubscriptionId，则添加id用于更新
            if(isEdit && item.strategySubscriptionId){
                signal.id = item.strategySubscriptionId;
            }
            return signal;
        });
        
        // 验证JSON格式
        try {
            if (formData.entryCondition && formData.entryCondition.trim()) {
                JSON.parse(formData.entryCondition);
            }
            if (formData.exitCondition && formData.exitCondition.trim()) {
                JSON.parse(formData.exitCondition);
            }
        } catch (e) {
            layer.close(loading);
            layer.msg('入场条件或退出条件的JSON格式不正确: ' + e.message, {icon: 2});
            return false;
        }
        
        // 构建完整的策略请求
        var requestData = {
            definition: {
                strategyName: formData.strategyName,
                strategyType: formData.strategyType,
                decisionMode: formData.decisionMode || '',
                enabled: isEdit ? 0 : 1 // 编辑时保持禁用状态
            },
            param: {
                initialCapital: parseFloat(formData.initialCapital),
                baseOrderRatio: parseFloat(formData.baseOrderRatio),
                takeProfitRatio: formData.takeProfitRatio ? parseFloat(formData.takeProfitRatio) : null,
                stopLossRatio: formData.stopLossRatio ? parseFloat(formData.stopLossRatio) : null,
                entryCondition: formData.entryCondition && formData.entryCondition.trim() ? formData.entryCondition : null,
                exitCondition: formData.exitCondition && formData.exitCondition.trim() ? formData.exitCondition : null
            },
            tradingPairs: selectedPairs,
            signalSubscriptions: selectedSignals
        };
        
        // 根据是否有strategyId决定调用创建还是更新接口
        var url = isEdit ? '/api/strategy/definition/update-complete/' + strategyId : '/api/strategy/definition/create-complete';
        var method = isEdit ? 'PUT' : 'POST';
        var successMsg = isEdit ? '策略更新成功！' : '策略创建成功！';
        var errorMsg = isEdit ? '策略更新失败' : '策略创建失败';
        
        request({
            url: url,
            method: method,
            data: requestData
        }).then(function(res){
            layer.close(loading);
            if (res.code === 200) {
                layer.msg(successMsg, {icon: 1, time: 2000});
                layer.closeAll('page');
                loadStrategies();
            } else {
                layer.msg(res.message || errorMsg, {icon: 2});
            }
        }).catch(function(err){
            layer.close(loading);
            var errorMsgText = err.message || '未知错误';
            if (err.responseJSON && err.responseJSON.message) {
                errorMsgText = err.responseJSON.message;
            }
            layer.msg(errorMsg + ': ' + errorMsgText, {icon: 2, time: 3000});
        });
        
        return false;
    });
    
    // 编辑策略表单提交（事务性）
    form.on('submit(submitEditStrategy)', function(data){
        var loading = layer.load(1);
        var formData = data.field;
        var strategyId = $('#editStrategyId').val();
        
        // 使用已选中的交易对列表
        var selectedPairs = editSelectedTradingPairs.map(function(item){
            return {
                id: item.id, // 如果有id则是更新，没有则是新增
                tradingPairId: item.tradingPairId,
                enabled: item.enabled ? 1 : 0
            };
        });
        
        // 使用已选中的信号配置列表
        var selectedSignals = editSelectedSignalConfigs.map(function(item){
            return {
                id: item.id, // 如果有id则是更新，没有则是新增
                signalConfigId: item.signalConfigId,
                consumeMode: item.consumeMode || 'LATEST_ONLY',
                enabled: item.enabled ? 1 : 0
            };
        });
        
        // 验证JSON格式
        try {
            if (formData.entryCondition && formData.entryCondition.trim()) {
                JSON.parse(formData.entryCondition);
            }
            if (formData.exitCondition && formData.exitCondition.trim()) {
                JSON.parse(formData.exitCondition);
            }
        } catch (e) {
            layer.close(loading);
            layer.msg('入场条件或退出条件的JSON格式不正确: ' + e.message, {icon: 2});
            return false;
        }
        
        // 构建完整的策略更新请求
        var updateRequest = {
            definition: {
                strategyName: formData.strategyName,
                strategyType: formData.strategyType,
                decisionMode: formData.decisionMode || '',
                enabled: 0 // 编辑时不允许修改启用状态，保持禁用状态
            },
            param: {
                initialCapital: parseFloat(formData.initialCapital),
                baseOrderRatio: parseFloat(formData.baseOrderRatio),
                takeProfitRatio: formData.takeProfitRatio ? parseFloat(formData.takeProfitRatio) : null,
                stopLossRatio: formData.stopLossRatio ? parseFloat(formData.stopLossRatio) : null,
                entryCondition: formData.entryCondition && formData.entryCondition.trim() ? formData.entryCondition : null,
                exitCondition: formData.exitCondition && formData.exitCondition.trim() ? formData.exitCondition : null
            },
            tradingPairs: selectedPairs,
            signalSubscriptions: selectedSignals
        };
        
        // 一次性提交，后端事务处理
        request({
            url: '/api/strategy/definition/update-complete/' + strategyId,
            method: 'PUT',
            data: updateRequest
        }).then(function(res){
            layer.close(loading);
            if (res.code === 200) {
                layer.msg('策略更新成功！', {icon: 1, time: 2000});
                layer.closeAll('page');
                loadStrategies();
            } else {
                layer.msg(res.message || '策略更新失败', {icon: 2});
            }
        }).catch(function(err){
            layer.close(loading);
            var errorMsg = err.message || '未知错误';
            if (err.responseJSON && err.responseJSON.message) {
                errorMsg = err.responseJSON.message;
            }
            layer.msg('策略更新失败: ' + errorMsg, {icon: 2, time: 3000});
        });
        
        return false;
    });
    
    // 查看策略详情
    window.viewStrategyDetail = function(id){
        var loading = layer.load(1);
        
        request({url: '/api/strategy/definition/detail/' + id}).then(function(res){
            layer.close(loading);
            
            if(res.data){
                var detail = res.data;
                
                // 填充策略定义
                if(detail.definition){
                    $('#detailStrategyName').text(detail.definition.strategyName || '-');
                    $('#detailStrategyType').text(detail.definition.strategyType || '-');
                    $('#detailDecisionMode').text(detail.definition.decisionMode || '-');
                }
                
                // 填充策略参数
                if(detail.param){
                    $('#detailInitialCapital').text(detail.param.initialCapital || '-');
                    $('#detailBaseOrderRatio').text(detail.param.baseOrderRatio || '-');
                    $('#detailTakeProfitRatio').text(detail.param.takeProfitRatio || '-');
                    $('#detailStopLossRatio').text(detail.param.stopLossRatio || '-');
                    $('#detailEntryCondition').val(detail.param.entryCondition || '{"mode":"ANY","rules":[]}');
                    $('#detailExitCondition').val(detail.param.exitCondition || '{"mode":"ANY","rules":[]}');
                }
                
                // 填充交易对列表
                if(detail.tradingPairs && detail.tradingPairs.length > 0){
                    var html = '';
                    detail.tradingPairs.forEach(function(item){
                        var marketTypeText = item.marketType === 'SPOT' ? '现货' : (item.marketType === 'SWAP' ? '合约' : item.marketType);
                        var enabledText = item.enabled == 1 ? '<span style="color: #52c41a;">✓ 启用</span>' : '<span style="color: #8c8c8c;">✗ 禁用</span>';
                        html += '<tr>' +
                            '<td><strong>' + (item.tradingPairName || '-') + '</strong></td>' +
                            '<td><span class="layui-badge layui-bg-blue">' + marketTypeText + '</span></td>' +
                            '<td>' + enabledText + '</td>' +
                            '</tr>';
                    });
                    $('#detailTradingPairTableBody').html(html);
                } else {
                    $('#detailTradingPairTableBody').html('<tr><td colspan="3" class="empty-state"><i class="layui-icon layui-icon-file"></i><div>暂无交易对</div></td></tr>');
                }
                
                // 填充信号订阅列表
                if(detail.signalSubscriptions && detail.signalSubscriptions.length > 0){
                    var html = '';
                    detail.signalSubscriptions.forEach(function(item){
                        var enabledText = item.enabled == 1 ? '<span style="color: #52c41a;">✓ 启用</span>' : '<span style="color: #8c8c8c;">✗ 禁用</span>';
                        html += '<tr>' +
                            '<td><strong>' + (item.signalConfigName || '-') + '</strong></td>' +
                            '<td><span class="layui-badge" style="background: #52c41a;">TradingView</span></td>' +
                            '<td><span style="color: #595959; font-size: 12px;">' + (item.consumeMode || '-') + '</span></td>' +
                            '<td>' + enabledText + '</td>' +
                            '</tr>';
                    });
                    $('#detailSignalConfigTableBody').html(html);
                } else {
                    $('#detailSignalConfigTableBody').html('<tr><td colspan="4" class="empty-state"><i class="layui-icon layui-icon-file"></i><div>暂无信号源</div></td></tr>');
                }
                
                // 打开详情弹窗
                layer.open({
                    type: 1,
                    title: '策略详情',
                    area: ['1400px', '95%'],
                    content: $('#strategyDetailForm'),
                    success: function(layero, index){
                        // 详情弹窗不需要渲染表单
                    }
                });
            } else {
                layer.msg('获取策略详情失败', {icon: 2});
            }
        }).catch(function(err){
            layer.close(loading);
            layer.msg('获取策略详情失败: ' + (err.message || '未知错误'), {icon: 2});
        });
    };
    
    // ==================== 条件构建器逻辑 ====================
    
    // 因子选项
    var FACTOR_OPTIONS = {
        'SIG.DIRECTION': '信号方向',
        'SIG.INTENT_ID': '信号意图ID',
        'IND.RSI_14': 'RSI指标',
        'IND.MACD': 'MACD指标',
        'IND.MACD.SIGNAL': 'MACD信号线',
        'BAR.OPEN': 'K线开盘价',
        'BAR.HIGH': 'K线最高价',
        'BAR.LOW': 'K线最低价',
        'BAR.CLOSE': 'K线收盘价',
        'BAR.VOLUME': 'K线成交量',
        'PX.LAST': '最新价格',
        'STATE.POSITION_SIDE': '持仓方向',
        'STATE.POSITION_QTY': '持仓数量',
        'STATE.STOP_LOSS_PRICE': '止损价格'
    };
    
    // 操作符选项
    var OPERATOR_OPTIONS = {
        'GT': '大于 (>)',
        'LT': '小于 (<)',
        'GTE': '大于等于 (>=)',
        'LTE': '小于等于 (<=)',
        'EQ': '等于 (=)',
        'NEQ': '不等于 (!=)',
        'CONTAINS': '包含',
        'STARTS_WITH': '以...开头',
        'ENDS_WITH': '以...结尾'
    };
    
    // 类型选项
    var TYPE_OPTIONS = {
        'NUMBER': '数值',
        'STRING': '字符串',
        'BOOLEAN': '布尔值'
    };
    
    // 初始化条件构建器
    function initConditionBuilder(builderId, jsonId) {
        var $builder = $('#' + builderId);
        var $json = $('#' + jsonId);
        
        // 从JSON解析并渲染
        try {
            var conditionJson = $json.val() || '{"version":"1.0","mode":"ALL","rules":[],"groups":[]}';
            var condition = JSON.parse(conditionJson);
            renderConditionBuilder($builder, condition, jsonId);
        } catch (e) {
            console.error('解析条件JSON失败:', e);
            var defaultCondition = {version: "1.0", mode: "ALL", rules: [], groups: []};
            renderConditionBuilder($builder, defaultCondition, jsonId);
        }
    }
    
    // 渲染条件构建器
    function renderConditionBuilder($container, condition, jsonId) {
        var html = '<div class="condition-group" data-group-index="0">';
        html += '<div class="condition-group-header">';
        html += '<div class="condition-group-title">';
        html += '<span class="mode-badge ' + (condition.mode || 'ALL') + '">' + (condition.mode || 'ALL') + '</span>';
        html += '<span>组合模式</span>';
        html += '</div>';
        html += '<div class="condition-group-actions">';
        html += '<select class="mode-select" onchange="updateConditionMode(this, \'' + jsonId + '\')">';
        html += '<option value="ALL"' + ((condition.mode || 'ALL') === 'ALL' ? ' selected' : '') + '>ALL（全部满足）</option>';
        html += '<option value="ANY"' + ((condition.mode || 'ALL') === 'ANY' ? ' selected' : '') + '>ANY（任一满足）</option>';
        html += '</select>';
        html += '<button type="button" class="layui-btn layui-btn-xs layui-btn-danger" onclick="deleteConditionGroup(this, \'' + jsonId + '\')"><i class="layui-icon layui-icon-delete"></i> 删除组</button>';
        html += '</div>';
        html += '</div>';
        
        // 渲染规则
        html += '<div class="condition-rules-container">';
        if (condition.rules && condition.rules.length > 0) {
            condition.rules.forEach(function(rule, index) {
                html += renderConditionRule(rule, index, jsonId);
            });
        } else {
            html += '<div class="empty-condition"><i class="layui-icon layui-icon-add-circle"></i><div>暂无规则，点击下方按钮添加</div></div>';
        }
        html += '</div>';
        
        // 渲染嵌套组
        if (condition.groups && condition.groups.length > 0) {
            condition.groups.forEach(function(group, index) {
                html += renderConditionGroup(group, index, jsonId, true);
            });
        }
        
        // 添加规则和组按钮
        html += '<button type="button" class="add-rule-btn" onclick="addConditionRule(this, \'' + jsonId + '\')"><i class="layui-icon layui-icon-add-1"></i> 添加规则</button>';
        html += '<button type="button" class="add-group-btn" onclick="addConditionGroup(this, \'' + jsonId + '\')"><i class="layui-icon layui-icon-add-1"></i> 添加嵌套组</button>';
        
        html += '</div>';
        
        $container.html(html);
        
        // 渲染下拉框
        form.render('select');
    }
    
    // 渲染单个规则
    function renderConditionRule(rule, index, jsonId) {
        var html = '<div class="condition-rule" data-rule-index="' + index + '">';
        html += '<div class="drag-handle"><i class="layui-icon layui-icon-drag"></i></div>';
        html += '<div class="condition-rule-content">';
        
        // 因子选择
        html += '<div class="rule-item">';
        html += '<label>因子：</label>';
        html += '<select class="rule-factor" onchange="updateConditionJson(\'' + jsonId + '\')">';
        for (var factor in FACTOR_OPTIONS) {
            html += '<option value="' + factor + '"' + (rule.factor === factor ? ' selected' : '') + '>' + FACTOR_OPTIONS[factor] + ' (' + factor + ')</option>';
        }
        html += '</select>';
        html += '</div>';
        
        // 操作符选择
        html += '<div class="rule-item">';
        html += '<label>操作符：</label>';
        html += '<select class="rule-operator" onchange="updateConditionJson(\'' + jsonId + '\')">';
        for (var op in OPERATOR_OPTIONS) {
            html += '<option value="' + op + '"' + (rule.operator === op ? ' selected' : '') + '>' + OPERATOR_OPTIONS[op] + '</option>';
        }
        html += '</select>';
        html += '</div>';
        
        // 比较值输入
        html += '<div class="rule-item">';
        html += '<label>值：</label>';
        html += '<input type="text" class="rule-value" value="' + (rule.value || '') + '" onchange="updateConditionJson(\'' + jsonId + '\')" placeholder="输入比较值">';
        html += '</div>';
        
        // 类型选择
        html += '<div class="rule-item">';
        html += '<label>类型：</label>';
        html += '<select class="rule-type" onchange="updateConditionJson(\'' + jsonId + '\')">';
        for (var type in TYPE_OPTIONS) {
            html += '<option value="' + type + '"' + ((rule.type || 'NUMBER') === type ? ' selected' : '') + '>' + TYPE_OPTIONS[type] + '</option>';
        }
        html += '</select>';
        html += '</div>';
        
        // 是否允许为空
        html += '<div class="rule-item">';
        html += '<label><input type="checkbox" class="rule-nullable"' + (rule.nullable === true ? ' checked' : '') + ' onchange="updateConditionJson(\'' + jsonId + '\')"> 允许为空</label>';
        html += '</div>';
        
        html += '</div>';
        html += '<div class="rule-actions">';
        html += '<button type="button" class="layui-btn layui-btn-xs layui-btn-danger" onclick="deleteConditionRule(this, \'' + jsonId + '\')"><i class="layui-icon layui-icon-delete"></i></button>';
        html += '</div>';
        html += '</div>';
        
        return html;
    }
    
    // 渲染嵌套组
    function renderConditionGroup(group, index, jsonId, isNested) {
        var html = '<div class="condition-group' + (isNested ? ' nested-group' : '') + '" data-group-index="' + index + '">';
        html += '<div class="condition-group-header">';
        html += '<div class="condition-group-title">';
        html += '<span class="mode-badge ' + (group.mode || 'ALL') + '">' + (group.mode || 'ALL') + '</span>';
        html += '<span>嵌套组</span>';
        html += '</div>';
        html += '<div class="condition-group-actions">';
        html += '<select class="mode-select" onchange="updateConditionMode(this, \'' + jsonId + '\', true, ' + index + ')">';
        html += '<option value="ALL"' + ((group.mode || 'ALL') === 'ALL' ? ' selected' : '') + '>ALL</option>';
        html += '<option value="ANY"' + ((group.mode || 'ALL') === 'ANY' ? ' selected' : '') + '>ANY</option>';
        html += '</select>';
        html += '<button type="button" class="layui-btn layui-btn-xs layui-btn-danger" onclick="deleteConditionGroup(this, \'' + jsonId + '\', ' + index + ')"><i class="layui-icon layui-icon-delete"></i></button>';
        html += '</div>';
        html += '</div>';
        
        // 渲染组的规则
        html += '<div class="condition-rules-container">';
        if (group.rules && group.rules.length > 0) {
            group.rules.forEach(function(rule, ruleIndex) {
                html += renderConditionRule(rule, ruleIndex, jsonId);
            });
        }
        html += '</div>';
        
        // 添加规则按钮
        html += '<button type="button" class="add-rule-btn" onclick="addConditionRuleToGroup(this, \'' + jsonId + '\', ' + index + ')"><i class="layui-icon layui-icon-add-1"></i> 添加规则</button>';
        
        html += '</div>';
        
        return html;
    }
    
    // 添加规则
    window.addConditionRule = function(btn, jsonId) {
        var $builder = $('#' + jsonId.replace('Json', 'Builder'));
        var condition = parseConditionFromBuilder($builder);
        if (!condition.rules) condition.rules = [];
        
        condition.rules.push({
            factor: 'IND.RSI_14',
            operator: 'LT',
            value: '',
            type: 'NUMBER',
            nullable: false
        });
        
        renderConditionBuilder($builder, condition, jsonId);
        updateConditionJson(jsonId);
    };
    
    // 添加规则到嵌套组
    window.addConditionRuleToGroup = function(btn, jsonId, groupIndex) {
        var $builder = $('#' + jsonId.replace('Json', 'Builder'));
        var condition = parseConditionFromBuilder($builder);
        if (!condition.groups) condition.groups = [];
        if (!condition.groups[groupIndex].rules) condition.groups[groupIndex].rules = [];
        
        condition.groups[groupIndex].rules.push({
            factor: 'IND.RSI_14',
            operator: 'LT',
            value: '',
            type: 'NUMBER',
            nullable: false
        });
        
        renderConditionBuilder($builder, condition, jsonId);
        updateConditionJson(jsonId);
    };
    
    // 添加嵌套组
    window.addConditionGroup = function(btn, jsonId) {
        var $builder = $('#' + jsonId.replace('Json', 'Builder'));
        var condition = parseConditionFromBuilder($builder);
        if (!condition.groups) condition.groups = [];
        
        condition.groups.push({
            mode: 'ALL',
            rules: [],
            groups: []
        });
        
        renderConditionBuilder($builder, condition, jsonId);
        updateConditionJson(jsonId);
    };
    
    // 删除规则
    window.deleteConditionRule = function(btn, jsonId) {
        var $rule = $(btn).closest('.condition-rule');
        var ruleIndex = $rule.data('rule-index');
        var $group = $rule.closest('.condition-group');
        var isNested = $group.hasClass('nested-group');
        var groupIndex = isNested ? $group.data('group-index') : null;
        
        var $builder = $('#' + jsonId.replace('Json', 'Builder'));
        var condition = parseConditionFromBuilder($builder);
        
        if (isNested && groupIndex !== null && condition.groups && condition.groups[groupIndex]) {
            condition.groups[groupIndex].rules.splice(ruleIndex, 1);
        } else {
            condition.rules.splice(ruleIndex, 1);
        }
        
        renderConditionBuilder($builder, condition, jsonId);
        updateConditionJson(jsonId);
    };
    
    // 删除组
    window.deleteConditionGroup = function(btn, jsonId, groupIndex) {
        if (confirm('确定要删除这个组吗？')) {
            var $builder = $('#' + jsonId.replace('Json', 'Builder'));
            var condition = parseConditionFromBuilder($builder);
            
            if (groupIndex !== undefined && condition.groups && condition.groups[groupIndex]) {
                condition.groups.splice(groupIndex, 1);
            } else {
                // 删除主组时，重置为默认条件
                condition = {version: "1.0", mode: "ALL", rules: [], groups: []};
            }
            
            renderConditionBuilder($builder, condition, jsonId);
            updateConditionJson(jsonId);
        }
    };
    
    // 更新组合模式
    window.updateConditionMode = function(select, jsonId, isNested, groupIndex) {
        var mode = $(select).val();
        var $builder = $('#' + jsonId.replace('Json', 'Builder'));
        var condition = parseConditionFromBuilder($builder);
        
        if (isNested && groupIndex !== undefined && condition.groups && condition.groups[groupIndex]) {
            condition.groups[groupIndex].mode = mode;
        } else {
            condition.mode = mode;
        }
        
        renderConditionBuilder($builder, condition, jsonId);
        updateConditionJson(jsonId);
    };
    
    // 从构建器解析条件对象
    function parseConditionFromBuilder($builder) {
        var $mainGroup = $builder.find('.condition-group').first();
        var condition = {
            version: "1.0",
            mode: $mainGroup.find('.mode-select').first().val() || "ALL",
            rules: [],
            groups: []
        };
        
        // 解析主组规则
        $mainGroup.find('.condition-rule').each(function() {
            var $rule = $(this);
            condition.rules.push({
                factor: $rule.find('.rule-factor').val(),
                operator: $rule.find('.rule-operator').val(),
                value: $rule.find('.rule-value').val(),
                type: $rule.find('.rule-type').val(),
                nullable: $rule.find('.rule-nullable').is(':checked')
            });
        });
        
        // 解析嵌套组
        $builder.find('.nested-group').each(function() {
            var $group = $(this);
            var groupIndex = $group.data('group-index');
            var group = {
                mode: $group.find('.mode-select').val() || "ALL",
                rules: [],
                groups: []
            };
            
            $group.find('.condition-rule').each(function() {
                var $rule = $(this);
                group.rules.push({
                    factor: $rule.find('.rule-factor').val(),
                    operator: $rule.find('.rule-operator').val(),
                    value: $rule.find('.rule-value').val(),
                    type: $rule.find('.rule-type').val(),
                    nullable: $rule.find('.rule-nullable').is(':checked')
                });
            });
            
            condition.groups.push(group);
        });
        
        return condition;
    }
    
    // 更新JSON
    window.updateConditionJson = function(jsonId) {
        var $builder = $('#' + jsonId.replace('Json', 'Builder'));
        var $json = $('#' + jsonId);
        var condition = parseConditionFromBuilder($builder);
        $json.val(JSON.stringify(condition, null, 2));
    };
    
    // 打开创建策略弹窗时初始化条件构建器
    window.openCreateStrategy = function() {
        // 重置已选列表
        selectedTradingPairs = [];
        selectedSignalConfigs = [];
        
        // 清空strategyId（创建模式）
        $('#createStrategyId').val('');
        
        // 重置表单
        $('#createStrategyForm form')[0].reset();
        $('#createStrategyId').val('');
        
        // 加载下拉框选项
        if (typeof loadTradingPairSelect === 'function') {
            loadTradingPairSelect();
        }
        if (typeof loadSignalConfigSelect === 'function') {
            loadSignalConfigSelect();
        }
        
        // 渲染已选列表
        if (typeof renderSelectedTradingPairs === 'function') {
            renderSelectedTradingPairs();
        }
        if (typeof renderSelectedSignalConfigs === 'function') {
            renderSelectedSignalConfigs();
        }
        
        // 初始化条件构建器
        initConditionBuilder('entryConditionBuilder', 'entryConditionJson');
        initConditionBuilder('exitConditionBuilder', 'exitConditionJson');
        
        form.render();
        
        layer.open({
            type: 1,
            title: '创建策略',
            area: ['1400px', '95%'],
            content: $('#createStrategyForm'),
            success: function(layero, index) {
                // 重新渲染所有表单元素，确保下拉框正常显示
                form.render();
                // 延迟一下确保渲染完成
                setTimeout(function(){
                    form.render('select');
                }, 50);
            }
        });
    };
    
    // 编辑策略时初始化条件构建器
    window.editStrategy = function(strategyId, enabled) {
        if (enabled == 1) {
            layer.msg('启用中的策略不可编辑', {icon: 0});
            return;
        }
        
        var loading = layer.load(1);
        $.ajax({
            url: '/api/strategy/detail',
            method: 'GET',
            data: {strategyId: strategyId},
            success: function(res) {
                layer.close(loading);
                if (res.success && res.data) {
                    var strategy = res.data;
                    $('#editStrategyId').val(strategy.id);
                    $('#editStrategyForm input[name="strategyName"]').val(strategy.strategyName);
                    $('#editStrategyForm select[name="strategyType"]').val(strategy.strategyType);
                    $('#editStrategyForm select[name="decisionMode"]').val(strategy.decisionMode || '');
                    
                    if (strategy.param) {
                        $('#editStrategyForm input[name="initialCapital"]').val(strategy.param.initialCapital);
                        $('#editStrategyForm input[name="baseOrderRatio"]').val(strategy.param.baseOrderRatio);
                        $('#editStrategyForm input[name="takeProfitRatio"]').val(strategy.param.takeProfitRatio || '');
                        $('#editStrategyForm input[name="stopLossRatio"]').val(strategy.param.stopLossRatio || '');
                        
                        // 初始化条件构建器
                        if (strategy.param.entryCondition) {
                            $('#editEntryConditionJson').val(strategy.param.entryCondition);
                        }
                        if (strategy.param.exitCondition) {
                            $('#editExitConditionJson').val(strategy.param.exitCondition);
                        }
                        initConditionBuilder('editEntryConditionBuilder', 'editEntryConditionJson');
                        initConditionBuilder('editExitConditionBuilder', 'editExitConditionJson');
                    }
                    
                    // 加载交易对和信号配置
                    loadEditStrategySymbols(strategyId);
                    loadEditStrategySignals(strategyId);
                    
                    form.render();
                    
                    layer.open({
                        type: 1,
                        title: '编辑策略',
                        area: ['1400px', '95%'],
                        content: $('#editStrategyForm'),
                        success: function(layero, index) {
                            form.render();
                        }
                    });
                } else {
                    layer.msg('获取策略详情失败', {icon: 2});
                }
            },
            error: function() {
                layer.close(loading);
                layer.msg('获取策略详情失败', {icon: 2});
            }
        });
    };
    
    // 初始化
    loadStrategies();
    
    // 初始化表单渲染
    form.render();
});
