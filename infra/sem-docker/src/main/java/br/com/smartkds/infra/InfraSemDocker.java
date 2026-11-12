package br.com.smartkds.infra;

import de.flapdoodle.embed.mongo.commands.MongodArguments;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.mongo.transitions.Mongod;
import de.flapdoodle.embed.mongo.transitions.RunningMongodProcess;
import de.flapdoodle.reverse.TransitionWalker;
import de.flapdoodle.reverse.transitions.Start;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.apache.qpid.server.SystemLauncher;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

/**
 * Infraestrutura local para desenvolvimento SEM Docker (equivalente ao docker-compose):
 *   PostgreSQL 16 em localhost:5432 (bancos smartkds_pedido, smartkds_producao, smartkds_estabelecimento),
 *   MongoDB 7 em localhost:27017 e broker AMQP 0-9-1 (Apache Qpid Broker-J) em localhost:5672 (guest/guest).
 * Os dados ficam em infra/sem-docker/dados/ e sobrevivem a reinicios (exceto as filas, que ficam em memoria).
 */
public final class InfraSemDocker {

    public static void main(String[] args) throws Exception {
        Path dados = Path.of("dados").toAbsolutePath();
        Files.createDirectories(dados.resolve("postgres"));

        boolean novoBanco = !Files.exists(dados.resolve("postgres").resolve("PG_VERSION"));
        EmbeddedPostgres postgres = EmbeddedPostgres.builder()
                .setPort(5432)
                .setDataDirectory(dados.resolve("postgres"))
                .setCleanDataDirectory(false)
                .start();
        if (novoBanco) {
            try (Connection c = postgres.getPostgresDatabase().getConnection(); Statement s = c.createStatement()) {
                for (String servico : new String[] {"pedido", "producao", "estabelecimento"}) {
                    s.execute("CREATE ROLE " + servico + " LOGIN PASSWORD '" + servico + "' NOSUPERUSER");
                    s.execute("CREATE DATABASE smartkds_" + servico + " OWNER " + servico);
                }
            }
        }
        System.out.println(">> PostgreSQL 16 em localhost:5432");

        TransitionWalker.ReachedState<RunningMongodProcess> mongo = Mongod.instance()
                .withNet(Start.to(Net.class).initializedWith(Net.defaults().withPort(27017)))
                .withMongodArguments(Start.to(MongodArguments.class).initializedWith(MongodArguments.defaults()))
                .start(Version.Main.V7_0);
        System.out.println(">> MongoDB 7 em localhost:27017");

        Map<String, Object> atributos = new HashMap<>();
        atributos.put("type", "Memory");
        atributos.put("initialConfigurationLocation",
                InfraSemDocker.class.getResource("/qpid-config.json").toExternalForm());
        atributos.put("startupLoggedToSystemOut", false);
        atributos.put("qpid.work_dir", dados.resolve("qpid").toString());
        SystemLauncher broker = new SystemLauncher();
        broker.startup(atributos);
        System.out.println(">> Broker AMQP 0-9-1 em localhost:5672 (guest/guest)");
        System.out.println(">> Infraestrutura pronta. Ctrl+C para encerrar.");

        CountDownLatch fim = new CountDownLatch(1);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            broker.shutdown();
            mongo.close();
            try {
                postgres.close();
            } catch (Exception ignorado) {
                // encerrando
            }
            fim.countDown();
        }));
        fim.await();
    }
}
