package bootiful.dewdrop;

import events.dewdrop.Dewdrop;
import events.dewdrop.aggregate.annotation.Aggregate;
import events.dewdrop.aggregate.annotation.AggregateId;
import events.dewdrop.api.result.Result;
import events.dewdrop.api.validators.ValidationException;
import events.dewdrop.command.CommandHandler;
import events.dewdrop.config.DependencyInjectionAdapter;
import events.dewdrop.config.DewdropProperties;
import events.dewdrop.config.DewdropSettings;
import events.dewdrop.read.readmodel.annotation.*;
import events.dewdrop.read.readmodel.query.QueryHandler;
import events.dewdrop.read.readmodel.stream.StreamType;
import events.dewdrop.structure.api.Command;
import events.dewdrop.structure.api.Event;
import events.dewdrop.structure.api.validator.DewdropValidator;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Simple application demonstrating how to use the Dewdrop event sourcing framework
 *
 * @author Josh Long
 */
@SpringBootApplication
public class DewdropApplication {

    public static void main(String[] args) {
        SpringApplication.run(DewdropApplication.class, args);
    }

}


@Configuration
class DewdropConfiguration {

    @Bean
    DewdropDependencyInjection dependencyInjection(
            ApplicationContext applicationContext) {
        return new DewdropDependencyInjection(applicationContext);
    }

    @Bean
    DewdropProperties dewdropProperties() {
        //  var packages = AutoConfigurationPackages.get(beanFactory) ;
        return DewdropProperties.builder()
                .packageToScan("bootiful.dewdrop")
                .connectionString("esdb://localhost:2113?tls=false")
                .create();
    }

    @Bean
    Dewdrop dewdrop(DewdropProperties properties, DependencyInjectionAdapter adapter) {
        return DewdropSettings.builder()
                .properties(properties)
                .dependencyInjectionAdapter(adapter)
                .create()
                .start();
    }
}

abstract class DewdropAccountCommand extends Command {
    public UUID getAccountId() {
        return accountId;
    }

    @AggregateId
    private UUID accountId;

    public DewdropAccountCommand(UUID accountId) {
        super();
        this.accountId = accountId;
    }
}

class DewdropCreateAccountCommand extends DewdropAccountCommand {

    private String name;

    private UUID userId;


    public String getName() {
        return name;
    }

    public UUID getUserId() {
        return userId;
    }

    public DewdropCreateAccountCommand(UUID accountId, String name, UUID userId) {
        super(accountId);
        this.name = name;
        this.userId = userId;
    }
}

abstract class DewdropAccountEvent extends Event {

    public UUID getAccountId() {
        return accountId;
    }

    @AggregateId
    private UUID accountId;

    DewdropAccountEvent(UUID accountId) {
        this.accountId = accountId;
    }
}

@CreationEvent
class DewdropAccountCreated extends DewdropAccountEvent {

    private String name;
    private UUID userId;

    DewdropAccountCreated(UUID accountId, String name, UUID userId) {
        super(accountId);
        this.name = name;
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public UUID getUserId() {
        return userId;
    }
}

@ReadModel
@Stream(name = "DewdropFundsAddedToAccount", streamType = StreamType.EVENT)
@Stream(name = "DewdropAccountCreated", streamType = StreamType.EVENT)
class DewdropAccountSummaryReadModel {

    @DewdropCache
    DewdropAccountSummary dewdropAccountSummary;

    @QueryHandler
    public DewdropAccountSummary handle(DewdropAccountSummaryQuery query) {
// todo
        return dewdropAccountSummary;
    }
}

@ReadModel(ephemeral = true, destroyInMinutesUnused = ReadModel.DESTROY_IMMEDIATELY)
@Stream(name = "DewdropAccountAggregate", subscribed = true)
@Stream(name = "DewdropUserAggregate", subscribed = false)
class DewdropAccountDetailsReadModel {


    @DewdropCache
    Map<UUID, DewdropAccountDetails> cache;

    @QueryHandler
    public Result<DewdropAccountDetails> handle(DewdropGetAccountByIdQuery query) {
        DewdropAccountDetails dewdropAccountDetails = cache.get(query.getAccountId());
        if (dewdropAccountDetails != null) {
            return Result.of(dewdropAccountDetails);
        }
        return Result.empty();
    }
}


class DewdropAccountDetails {

    @PrimaryCacheKey
    private UUID accountId;

    private String name;

    private BigDecimal balance = BigDecimal.ZERO;

    @ForeignCacheKey
    private UUID userId;

    private String username;

    @EventHandler
    public void on(DewdropAccountCreated event) {
        this.accountId = event.getAccountId();
        this.name = event.getName();
        this.userId = event.getUserId();
    }

    @EventHandler
    public void on(DewdropFundsAddedToAccount event) {
        this.balance = this.balance.add(event.getFunds());
    }

    @EventHandler
    public void on(DewdropUserCreated userCreated) {
        this.username = userCreated.getUsername();
    }
}

class DewdropGetAccountByIdQuery {

    private UUID accountId;

    public UUID getAccountId() {
        return accountId;
    }
}

@Aggregate
class DewdropAccountAggregate {

    @AggregateId
    UUID accountId;

    String name;

    BigDecimal balance = BigDecimal.ZERO;

    public DewdropAccountAggregate() {
    }

    @CommandHandler
    public List<DewdropAccountCreated> handle(DewdropCreateAccountCommand command) throws ValidationException {
        DewdropValidator.validate(command);

        return List.of(new DewdropAccountCreated(command.getAccountId(), command.getName(), command.getUserId()));
    }

    @CommandHandler
    public List<DewdropFundsAddedToAccount> handle(DewdropAddFundsToAccountCommand command) {
        if (command.getAccountId() == null) {
            throw new IllegalArgumentException("Id cannot be empty");
        }

        DewdropFundsAddedToAccount dewdropFundsAddedToAccount = new DewdropFundsAddedToAccount(command.getAccountId(), command.getFunds());
        return List.of(dewdropFundsAddedToAccount);
    }

    @EventHandler
    public void on(DewdropAccountCreated event) {
        // validate here as well different
        // check that teh aggregate invariance are always true
        this.accountId = event.getAccountId();
        this.name = event.getName();
        // DewdropAccountAggregate.from(this).with();
    }

    @EventHandler
    public void on(DewdropFundsAddedToAccount event) {
        // this.accountId = event.getAccountId();
        this.balance = this.balance.add(event.getFunds());
    }
}

class DewdropDependencyInjection implements DependencyInjectionAdapter {

    private final ApplicationContext applicationContext;

    DewdropDependencyInjection(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    public <T> T getBean(Class<?> clazz) {
        return (T) applicationContext.getBean(clazz);
    }
}