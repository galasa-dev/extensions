ARG dockerRepository
ARG branch
FROM ${dockerRepository}/galasadev/galasa-maven-framework:${branch}

RUN sed -i 's/\/framework"/\/extensions"/' /usr/local/apache2/conf/httpd.conf

COPY repo/ /usr/local/apache2/htdocs/
COPY extensions.githash /usr/local/apache2/htdocs/extensions.githash
