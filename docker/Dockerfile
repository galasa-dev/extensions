ARG dockerRepository
ARG branch
FROM ${dockerRepository}/galasadev/galasa-maven-framework:${branch}

COPY repo/ /usr/local/apache2/htdocs/
COPY extensions.githash /usr/local/apache2/htdocs/extensions.githash
