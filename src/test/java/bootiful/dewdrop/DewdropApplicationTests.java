package bootiful.dewdrop;

import events.dewdrop.Dewdrop;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;

@SpringBootTest
class DewdropApplicationTests {

    @Test
    DewdropCreateAccountCommand createAccount(@Autowired Dewdrop dewdrop)
            throws Exception {
        var command = new DewdropCreateAccountCommand(UUID.randomUUID(), "test", UUID.randomUUID());
        dewdrop.executeCommand(command);

        DewdropGetAccountByIdQuery query = new DewdropGetAccountByIdQuery(accountId);
        dewdrop.executeQuery(query);



        return command;
    }
}
