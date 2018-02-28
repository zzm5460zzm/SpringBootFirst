package xyz.thelostsoul.base;

import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLStatement;
import org.apache.ibatis.executor.statement.RoutingStatementHandler;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.thelostsoul.annotation.TableTag;
import xyz.thelostsoul.base.split.inter.ISplitFieldParser;

import java.lang.annotation.Annotation;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

@Intercepts({
        @Signature(type = StatementHandler.class, method = "prepare", args = {Connection.class, Integer.class})
})
public class TableSplitInterceptor implements Interceptor {

    private final Logger LOG = LoggerFactory.getLogger(getClass());

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        RoutingStatementHandler statementHandler = (RoutingStatementHandler) invocation.getTarget();
        Object[] args = invocation.getArgs();

        Connection currentConnection = (Connection) args[0];
        DatabaseMetaData metaData = currentConnection.getMetaData();
        String dbType = metaData.getDatabaseProductName();

        BoundSql boundSql = statementHandler.getBoundSql();
        String sql = boundSql.getSql();
        LOG.info("原SQL：" + sql);

        MetaObject metaStatementHandler = SystemMetaObject.forObject(statementHandler);
        MappedStatement mappedStatement = (MappedStatement) metaStatementHandler.getValue("delegate.mappedStatement");
        String id = mappedStatement.getId();
        String mapperName = id.substring(0, id.lastIndexOf("."));
        Class mapper = Class.forName(mapperName);

        Annotation[] annotations = mapper.getAnnotations();
        if (annotations != null && annotations.length > 0) {
            Map<String, String> tableIndexMap = new HashMap<>(3);
            Object obj = boundSql.getParameterObject();
            Map<String, Object> params = null;
            if (obj instanceof Map) {
                params = (Map<String, Object>) obj;
            }
            for (Annotation annotation : annotations) {
                if (annotation instanceof TableTag) {
                    String tableName = ((TableTag) annotation).tableName();
                    String separator = ((TableTag) annotation).separator();
                    String shardByField = ((TableTag) annotation).shardByField();
                    Class<? extends ISplitFieldParser> fieldParserClass = ((TableTag) annotation).fieldParser();

                    ISplitFieldParser fieldParser = fieldParserClass.newInstance();
                    Object field = params.get(shardByField);
                    if (field != null) {
                        String tableIndex = fieldParser.convert(field);
                        tableIndexMap.put(tableName, separator + tableIndex);
                    } else {
                        throw new Exception("查询参数列表中没有可以确定分表的参数：" + tableName + "." +shardByField);
                    }
                }
            }

            List<SQLStatement> statementList = SQLUtils.parseStatements(sql, dbType);
            SplitTableExprVisitor visitor = new SplitTableExprVisitor(tableIndexMap);
            for (SQLStatement sqlStatement : statementList) {
                sqlStatement.accept(visitor);
            }
            sql = SQLUtils.toSQLString(statementList, dbType);
            LOG.info("执行分表规则后的SQL：" + sql);
            metaStatementHandler.setValue("delegate.boundSql.sql", sql);

        }

        return invocation.proceed();
    }

    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }

    @Override
    public void setProperties(Properties properties) {

    }
}
