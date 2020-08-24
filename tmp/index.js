process.env.NODE_TLS_REJECT_UNAUTHORIZED = '0'

const Jenkins = require('jenkins')
const axios = require('axios')

const { token } = require('./credentials.json')

const main = async () => {
  // const options = {
  //   method: 'GET',
  //   baseURL: 'https://cicddev.kasikornbank.com',
  //   headers: { 'Authorization': `Basic ${token}` },
  //   url: '/crumbIssuer/api/json',
  // };
  
  // console.log(options)

  // const { data } = await axios(options);
  // const { crumb, crumbRequestField } = data
  // console.log(data)
  
  const jenkinsOpts = {
    baseUrl: 'https://cicddev.kasikornbank.com',
    crumbIssuer: false,
    promisify: true,
    headers: { 'Authorization': `Basic ${token}` },
  }
  //jenkinsOpts.headers[`${crumbRequestField}`] = crumb
  console.log(jenkinsOpts)
  console.log('////////////////')
  const jenkins = Jenkins(jenkinsOpts);

  const resp = await jenkins.info()
  console.log(resp)
}

main()