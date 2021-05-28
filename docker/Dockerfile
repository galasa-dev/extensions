ARG frameworkVersion
ARG dockerRepository
ARG branch
FROM ${dockerRepository}/galasadev/galasa-maven-framework:${frameworkVersion}-${branch}

ARG gitHash

RUN sed -i 's/\/framework"/\/extensions"/' /usr/local/apache2/conf/httpd.conf

COPY repo/ /usr/local/apache2/htdocs/
RUN echo -n ${gitHash} > /usr/local/apache2/htdocs/extensions.githash
