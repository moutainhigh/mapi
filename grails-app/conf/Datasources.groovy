import org.codehaus.groovy.grails.commons.*

datasources = {
    datasource (name: 'ismp') {
        domainClasses([
                customer.Customer,
                customer.CustomerOperator,
                customer.DynamicKey,
                customer.LoginCertificate,
                customer.LoginLog,
                customer.RoyaltyBinding,
                customer.BindingMoney,
                customer.RefundDetail,
                gateway.GwOrder,
                gateway.Gwpayments,
                gateway.GwTransaction,
                gateway.Gwsuborders,
                mapi.AsyncNotify,
                trade.AccountCommandSaf,
                trade.TradeCharge,
                trade.TradeBase,
                trade.TradeFrozen,
                trade.TradePayment,
                trade.TradeRefund,
                trade.TradeTransfer,
                trade.TradeUnfrozen,
                trade.TradeWithdrawn
        ])
        driverClassName('oracle.jdbc.OracleDriver')
        url(ConfigurationHolder.config.dataSource.ismp.url)
        username(ConfigurationHolder.config.dataSource.ismp.username)
        password(DESCodec.decode(ConfigurationHolder.config.dataSource.ismp.password))
        dbCreate(ConfigurationHolder.config.dataSource.ismp.dbCreate)
        pooled(true)
        logSql(false)
        dialect(org.hibernate.dialect.Oracle10gDialect)
        hibernate {
            cache {
                use_second_level_cache(true)
                use_query_cache(true)
                provider_class('net.sf.ehcache.hibernate.EhCacheProvider')
            }
        }
    }


    datasource(name: 'account') {
        domainClasses([
                account.AcAccount
        ])
        driverClassName('oracle.jdbc.OracleDriver')
        url(ConfigurationHolder.config.dataSource.account.url)
        username(ConfigurationHolder.config.dataSource.account.username)
        password(ConfigurationHolder.config.dataSource.account.password)
        dbCreate(ConfigurationHolder.config.dataSource.account.dbCreate)
        pooled(true)
        logSql(true)
        dialect(org.hibernate.dialect.Oracle10gDialect)
        hibernate {
            cache {
                use_second_level_cache(true)
                use_query_cache(true)
                provider_class('net.sf.ehcache.hibernate.EhCacheProvider')
            }
        }
    }

    datasource(name: 'boss') {
        domainClasses([
                boss.InnerAccount,
                customer.CustomerService
        ])
        driverClassName('oracle.jdbc.OracleDriver')
        url(ConfigurationHolder.config.dataSource.boss.url)
        username(ConfigurationHolder.config.dataSource.boss.username)
        password(DESCodec.decode(ConfigurationHolder.config.dataSource.boss.password))
        dbCreate(ConfigurationHolder.config.dataSource.boss.dbCreate)
        pooled(true)
        logSql(false)
        dialect(org.hibernate.dialect.Oracle10gDialect)
        hibernate {
            cache {
                use_second_level_cache(true)
                use_query_cache(true)
                provider_class('net.sf.ehcache.hibernate.EhCacheProvider')
            }
        }
    }
}
