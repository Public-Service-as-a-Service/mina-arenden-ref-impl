# Engångsjobb som körs vid varje (om)deploy innan applikationen startar: säkerhetskopierar databasen och ser till
# att applikationens databasanvändare har lösenordet från DB_PASSWORD. Se docker/db-prepare/db-prepare.sh.
FROM mariadb:11.4
COPY db-prepare.sh /usr/local/bin/db-prepare.sh
RUN chmod 0755 /usr/local/bin/db-prepare.sh
ENTRYPOINT ["/usr/local/bin/db-prepare.sh"]
