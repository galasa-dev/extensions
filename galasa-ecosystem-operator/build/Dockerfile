FROM registry.access.redhat.com/ubi8/ubi-minimal:latest

ENV OPERATOR=/usr/local/bin/galasa-ecosystem-operator \
    USER_UID=1001 \
    USER_NAME=galasa-ecosystem-operator

RUN mkdir /usr/local/bin/galasa-resources
# install operator binary
COPY build/_output/bin/galasa-ecosystem-operator ${OPERATOR}
COPY resources /usr/local/bin/galasa-resources

COPY build/bin /usr/local/bin
RUN  /usr/local/bin/user_setup

ENTRYPOINT ["/usr/local/bin/entrypoint"]

USER ${USER_UID}
