package bootiful.dewdrop;

import events.dewdrop.Dewdrop;
import events.dewdrop.aggregate.annotation.Aggregate;
import events.dewdrop.aggregate.annotation.AggregateId;
import events.dewdrop.api.validators.ValidationException;
import events.dewdrop.command.CommandHandler;
import events.dewdrop.config.DependencyInjectionAdapter;
import events.dewdrop.config.DewdropProperties;
import events.dewdrop.config.DewdropSettings;
import events.dewdrop.read.readmodel.annotation.EventHandler;
import events.dewdrop.read.readmodel.annotation.ReadModel;
import events.dewdrop.structure.api.Command;
import events.dewdrop.structure.api.Event;
import events.dewdrop.structure.api.validator.DewdropValidator;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;

/**
 * Simple application demonstrating how to use the Dewdrop event sourcing framework
 *
 * @author Josh Long
 */
@SpringBootApplication
@EnableConfigurationProperties(DewdropConfigurationProperties.class)
public class DewdropApplication {

    public static void main(String[] args) {
        SpringApplication.run(DewdropApplication.class, args);
    }

}


@ConfigurationProperties(prefix = "dewdrop")
record DewdropConfigurationProperties(String eventStoreDbConnectionUrl) {
}

/**
 * Wire up Dewdrop.
 *
 * @author Josh Long
 */
@Configuration
class DewdropAutoConfiguration {

    @Bean
    DewdropDependencyInjection dewdropDependencyInjection(ApplicationContext context) {
        return new DewdropDependencyInjection(context);
    }

    /// todo rewrite this using ConnectionDetail in Spring Boot 3.1
    @Bean
    DewdropProperties dewdropProperties(
            DewdropConfigurationProperties properties,
            BeanFactory beanFactory) throws Exception {

        var url = properties.eventStoreDbConnectionUrl();

        if (url == null)
            url = "esdb://localhost:2113?tls=false";

        // todo it's kinda weird that we can only use the first auto configured package?
        //  should we give the user a way to specify a package in an annotation or something?
        var packages = AutoConfigurationPackages.get(beanFactory).get(0);

        return DewdropProperties//
                .builder()//
                .packageToScan(packages)//
                .connectionString(url)//
                .create();
    }

    @Bean
    Dewdrop dewdrop(DewdropDependencyInjection dewdropDependencyInjection,
                    DewdropProperties properties) {
        return DewdropSettings//
                .builder()//
                .properties(properties)//
                .dependencyInjectionAdapter(dewdropDependencyInjection)//
                .create()//
                .start();
    }
}

/**
 * adapter for the Dewdrop CQRS framework
 */
class DewdropDependencyInjection implements DependencyInjectionAdapter {

    private final ApplicationContext context;

    DewdropDependencyInjection(ApplicationContext applicationContext) {
        this.context = applicationContext;
    }

    @Override
    public <T> T getBean(Class<?> clazz) {
        System.out.println("DI: " + clazz.getName());
        return (T) context.getBean(clazz);
    }
}


class CartCreatedEvent extends Event {

    private final String accountId;
    private final Date when;

    CartCreatedEvent(String accountId, Date when) {
        this.accountId = accountId;
        this.when = when;
    }

    public String getAccountId() {
        return accountId;
    }

    public Date getWhen() {
        return when;
    }
}

class NewLineItemEvent extends Event {


}

/**
 * create the new shopping cart
 */
class CreateCartCommand extends Command {

    private final String accountId;

    private final Date when;

    CreateCartCommand(String accountId, Date when) {
        this.accountId = accountId;
        this.when = when;
    }

    public String getAccountId() {
        return accountId;
    }

    public Date getWhen() {
        return when;
    }
}

@Aggregate
class CartAggregate {

    private @AggregateId String accountId;
    private Date when;

    @CommandHandler
    public List<CartCreatedEvent> handleCartCreatedCommand(CreateCartCommand command) throws ValidationException {
        DewdropValidator.validate(command);
        return List.of(new CartCreatedEvent(command.getAccountId(), command.getWhen());
    }

    @EventHandler
    public void on(CartCreatedEvent cce) {
        this.accountId = cce.getAccountId();
        this.when = cce.getWhen();
    }
}

@ReadModel
@Component
class SimpleReadModel {

    SimpleReadModel() {
        System.out.println("starting readmodel..");
    }
}